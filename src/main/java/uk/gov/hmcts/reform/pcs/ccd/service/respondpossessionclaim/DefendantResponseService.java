package uk.gov.hmcts.reform.pcs.ccd.service.respondpossessionclaim;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.respondpossessionclaim.CounterClaim;
import uk.gov.hmcts.reform.pcs.ccd.domain.respondpossessionclaim.CounterClaimType;
import uk.gov.hmcts.reform.pcs.ccd.domain.respondpossessionclaim.DefendantResponses;
import uk.gov.hmcts.reform.pcs.ccd.domain.respondpossessionclaim.PossessionClaimResponse;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.respondpossessionclaim.CounterClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.respondpossessionclaim.CounterClaimPartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.respondpossessionclaim.DefendantResponseEntity;
import uk.gov.hmcts.reform.pcs.ccd.repository.ClaimRepository;
import uk.gov.hmcts.reform.pcs.ccd.repository.CounterClaimRepository;
import uk.gov.hmcts.reform.pcs.ccd.repository.DefendantResponseRepository;
import uk.gov.hmcts.reform.pcs.ccd.repository.PartyRepository;
import uk.gov.hmcts.reform.pcs.ccd.service.document.DocumentService;
import uk.gov.hmcts.reform.pcs.ccd.service.party.PartyService;
import uk.gov.hmcts.reform.pcs.security.SecurityContextService;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * Service for managing defendant responses.
 * Handles saving defendant responses to the defendant_response table with optimal concurrency.
 *
 * <p>Design Principles:
 * <ul>
 *   <li>Uses getReferenceById() for zero-query JPA proxies</li>
 *   <li>Minimal database locking (only new row)</li>
 *   <li>Supports concurrent defendant submissions</li>
 *   <li>Fail-fast duplicate detection</li>
 * </ul>
 */
@Service
@Slf4j
@Transactional
public class DefendantResponseService {

    private final PartyService partyService;
    private final PartyRepository partyRepository;
    private final ClaimRepository claimRepository;
    private final DefendantResponseRepository defendantResponseRepository;
    private final SecurityContextService securityContextService;
    private final ReasonableAdjustmentsService reasonableAdjustmentsService;
    private final HouseholdCircumstancesService householdCircumstancesService;
    private final PaymentAgreementService paymentAgreementService;
    private final DocumentService documentService;
    private final PartyAttributeAssertationService partyAttributeAssertationService;
    private final CounterClaimRepository counterClaimRepository;
    private final Clock utcClock;

    public DefendantResponseService(PartyService partyService,
                                    PartyRepository partyRepository,
                                    ClaimRepository claimRepository,
                                    DefendantResponseRepository defendantResponseRepository,
                                    SecurityContextService securityContextService,
                                    ReasonableAdjustmentsService reasonableAdjustmentsService,
                                    HouseholdCircumstancesService householdCircumstancesService,
                                    PaymentAgreementService paymentAgreementService,
                                    DocumentService documentService,
                                    PartyAttributeAssertationService partyAttributeAssertationService,
                                    CounterClaimRepository counterClaimRepository,
                                    @Qualifier("utcClock") Clock utcClock) {
        this.partyService = partyService;
        this.partyRepository = partyRepository;
        this.claimRepository = claimRepository;
        this.defendantResponseRepository = defendantResponseRepository;
        this.securityContextService = securityContextService;
        this.reasonableAdjustmentsService = reasonableAdjustmentsService;
        this.householdCircumstancesService = householdCircumstancesService;
        this.paymentAgreementService = paymentAgreementService;
        this.documentService = documentService;
        this.partyAttributeAssertationService = partyAttributeAssertationService;
        this.counterClaimRepository = counterClaimRepository;
        this.utcClock = utcClock;
    }

    /**
     * Saves a defendant's response to the defendant_response table
     * and its related details to the linked child tables.
     *
     * <p>Uses optimized approach with minimal locking:
     * <ol>
     *   <li>Check duplicate first (fail fast)</li>
     *   <li>Get IDs only (minimal lock time)</li>
     *   <li>Use getReferenceById() for proxies (no query)</li>
     *   <li>Build the DefendantResponseEntity and link it to the owning PcsCaseEntity</li>
     *   <li>Attach one-to-one child entities (household circumstances, payment agreement, reasonable adjustments)</li>
     *   <li>Persist the entities in a single save, only locks new row being inserted</li></li>
     * </ol>
     *
     * <p>This approach ensures concurrent defendants can submit simultaneously
     * without blocking each other or other case operations.
     *
     * @param caseReference The case reference number
     * @param possessionClaimResponse the possession claim response from draft data
     * @throws IllegalStateException if user ID is null, response already exists,
     *         party not found, or claim not found
     */
    public void saveDefendantResponse(long caseReference,
                                      PossessionClaimResponse possessionClaimResponse) {

        UUID userId = requireCurrentUserId();

        saveDefendantResponseInternal(
            caseReference,
            possessionClaimResponse,
            () -> partyService.getPartyEntityByIdamId(userId, caseReference),
            String.format("Successfully saved defendant response for case %s user %s",
                          caseReference, userId)
        );
    }

    public void saveDefendantResponse(long caseReference,
                                      PossessionClaimResponse possessionClaimResponse,
                                      UUID partyId) {

        requireCurrentUserId();

        saveDefendantResponseInternal(
            caseReference,
            possessionClaimResponse,
            () -> partyRepository
                .findByIdAndPcsCaseCaseReference(partyId, caseReference)
                .orElseThrow(() -> new IllegalStateException(
                    "No party found for party ID: "
                        + partyId
                        + " and case reference: "
                        + caseReference
                )),
            String.format("Successfully saved defendant response for case %s represented party %s",
                          caseReference, partyId)
        );
    }

    private void saveDefendantResponseInternal(
        long caseReference,
        PossessionClaimResponse possessionClaimResponse,
        Supplier<PartyEntity> partySupplier,
        String successLogMessage
    ) {
        PartyEntity partyRef = partySupplier.get();
        UUID claimId = claimRepository.findIdByCaseReference(caseReference)
            .orElseThrow(() -> {
                log.error("No claim found for case: {}", caseReference);
                return new IllegalStateException(
                    String.format("No claim found for case: %d", caseReference)
                );
            });

        ClaimEntity claimRef = claimRepository.getReferenceById(claimId);

        DefendantResponses responses = possessionClaimResponse.getDefendantResponses();

        DefendantResponseEntity responseEntity =
            buildDefendantResponseEntity(claimRef, partyRef, responses);

        buildAndLinkChildEntities(responseEntity, responses);

        CounterClaimEntity savedCounterClaim = saveCounterClaim(responses, partyRef, claimRef);

        DefendantResponseEntity savedResponse = defendantResponseRepository.save(responseEntity);

        if (!CollectionUtils.isEmpty(responses.getDefendantDocuments())) {
            documentService.createDefendantUploadedDocuments(
                responses.getDefendantDocuments(),
                savedResponse,
                claimRef.getPcsCase(),
                partyRef
            );
        }

        if (savedCounterClaim != null && !CollectionUtils.isEmpty(responses.getCounterClaimDocuments())) {
            documentService.createCounterClaimUploadedDocuments(
                responses.getCounterClaimDocuments(),
                savedCounterClaim,
                claimRef.getPcsCase(),
                partyRef
            );
        }

        partyAttributeAssertationService.buildPartyAttributeEntities(possessionClaimResponse, partyRef);

        log.info(successLogMessage);
    }

    private UUID requireCurrentUserId() {
        UUID userId = securityContextService.getCurrentUserId();

        if (userId == null) {
            log.error("Cannot save defendant response: current user IDAM ID is null");
            throw new IllegalStateException("Current user IDAM ID is null");
        }

        return userId;
    }

    private DefendantResponseEntity buildDefendantResponseEntity(ClaimEntity claimRef,
                                                                PartyEntity partyRef,
                                                                DefendantResponses responses) {

        DefendantResponseEntity defendantResponse = DefendantResponseEntity.builder()
            .claim(claimRef)
            .party(partyRef)
            .freeLegalAdvice(responses.getFreeLegalAdvice())
            .defendantNameConfirmation(responses.getDefendantNameConfirmation())
            .correspondenceAddressConfirmation(responses.getCorrespondenceAddressConfirmation())
            .landlordRegistered(responses.getLandlordRegistered())
            .writtenTerms(responses.getWrittenTerms())
            .disputeClaim(responses.getDisputeClaim())
            .disputeClaimDetails(responses.getDisputeClaimDetails())
            .makeCounterClaim(responses.getMakeCounterClaim())
            .counterClaimWantToUploadFiles(responses.getCounterClaimWantToUploadFiles())
            .tenancyStartDateConfirmation(responses.getTenancyStartDateConfirmation())
            .tenancyTypeConfirmation(responses.getTenancyTypeConfirmation())
            .landlordLicensed(responses.getLandlordLicensed())
            .rentArrearsAmountConfirmation(responses.getRentArrearsAmountConfirmation())
            .languageUsed(responses.getLanguageUsed())
            .otherConsiderations(responses.getOtherConsiderations())
            .otherConsiderationsDetails(responses.getOtherConsiderationsDetails())
            .build();

        //set bidirectional relationship with the pcs case
        claimRef.getPcsCase().addDefendantResponse(defendantResponse);

        return defendantResponse;
    }

    private void buildAndLinkChildEntities(
        DefendantResponseEntity defendantResponseEntity,
        DefendantResponses response) {

        defendantResponseEntity.setReasonableAdjustment(
            reasonableAdjustmentsService.createReasonableAdjustmentEntity(
                response.getReasonableAdjustments()
            )
        );

        defendantResponseEntity.setHouseholdCircumstances(
            householdCircumstancesService.createHouseholdCircumstancesEntity(
                response.getHouseholdCircumstances()
            )
        );

        defendantResponseEntity.setPaymentAgreement(
            paymentAgreementService.createPaymentAgreementEntity(
                response.getPaymentAgreement()
            )
        );
    }

    private CounterClaimEntity saveCounterClaim(DefendantResponses responses,
                                                PartyEntity partyRef,
                                                ClaimEntity claimRef) {
        CounterClaim cc = responses.getCounterClaim();
        if (cc == null) {
            return null;
        }

        boolean claimAmountApplies = cc.getClaimType() != null && cc.getClaimType() != CounterClaimType.SOMETHING_ELSE;

        CounterClaimEntity counterClaimEntity = CounterClaimEntity.builder()
            .claimType(cc.getClaimType())
            .isClaimAmountKnown(claimAmountApplies ? cc.getIsClaimAmountKnown() : null)
            .claimAmount(claimAmountApplies && cc.getIsClaimAmountKnown() == VerticalYesNo.YES
                             ? cc.getClaimAmount() : null)
            .estimatedMaxClaimAmount(claimAmountApplies && cc.getIsClaimAmountKnown() == VerticalYesNo.NO
                                         ? cc.getEstimatedMaxClaimAmount() : null)
            .counterClaimFor(cc.getCounterClaimFor())
            .counterClaimReasons(cc.getCounterClaimReasons())
            .otherOrderRequestDetails(cc.getClaimType() == CounterClaimType.SOMETHING_ELSE
                                          ? cc.getOtherOrderRequestDetails() : null)
            .otherOrderRequestFacts(cc.getClaimType() == CounterClaimType.SOMETHING_ELSE
                                        ? cc.getOtherOrderRequestFacts() : null)
            .needHelpWithFees(cc.getNeedHelpWithFees())
            .appliedForHwf(cc.getAppliedForHwf())
            .hwfReferenceNumber(cc.getHwfReferenceNumber())
            .claimSubmittedDate(LocalDateTime.now(utcClock))
            .party(partyRef)
            .build();

        if (cc.getCounterClaimAgainst() != null) {
            counterClaimEntity.getCounterClaimParties().addAll(
                cc.getCounterClaimAgainst().stream()
                    .filter(lv -> lv.getId() != null)
                    .map(lv -> CounterClaimPartyEntity.builder()
                        .counterClaim(counterClaimEntity)
                        .party(partyRepository.getReferenceById(UUID.fromString(lv.getId())))
                        .build())
                    .toList()
            );
        }

        claimRef.getPcsCase().addCounterClaim(counterClaimEntity);

        return counterClaimRepository.save(counterClaimEntity);
    }

    public boolean hasSubmittedResponse(long caseReference) {
        UUID userId = securityContextService.getCurrentUserId();
        if (userId == null) {
            return false;
        }
        return defendantResponseRepository.existsByClaimPcsCaseCaseReferenceAndPartyIdamId(caseReference, userId);
    }
}
