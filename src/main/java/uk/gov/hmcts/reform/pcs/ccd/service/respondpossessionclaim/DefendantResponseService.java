package uk.gov.hmcts.reform.pcs.ccd.service.respondpossessionclaim;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.reform.pcs.ccd.domain.YesNoNotSure;
import uk.gov.hmcts.reform.pcs.ccd.domain.respondpossessionclaim.DefendantResponses;
import uk.gov.hmcts.reform.pcs.ccd.domain.respondpossessionclaim.PossessionClaimResponse;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.respondpossessionclaim.DefendantResponseEntity;
import uk.gov.hmcts.reform.pcs.ccd.repository.ClaimRepository;
import uk.gov.hmcts.reform.pcs.ccd.repository.DefendantResponseRepository;
import uk.gov.hmcts.reform.pcs.ccd.repository.PartyRepository;
import uk.gov.hmcts.reform.pcs.ccd.service.DocumentService;
import uk.gov.hmcts.reform.pcs.ccd.service.party.PartyService;
import uk.gov.hmcts.reform.pcs.security.SecurityContextService;

import java.util.UUID;

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
@RequiredArgsConstructor
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
    public void saveDefendantResponse(long caseReference, PossessionClaimResponse possessionClaimResponse) {
        UUID userId = securityContextService.getCurrentUserId();

        if (userId == null) {
            log.error("Cannot save defendant response: current user IDAM ID is null");
            throw new IllegalStateException("Current user IDAM ID is null");
        }

        // Fail fast - check duplicate first (indexed query, very fast)
        if (defendantResponseRepository.existsByClaimPcsCaseCaseReferenceAndPartyIdamId(
                caseReference, userId)) {
            log.warn("Duplicate defendant response attempt for case {} user {}", caseReference, userId);
            throw new IllegalStateException("A response has already been submitted for this case.");
        }

        UUID partyId = partyService.getPartyEntityByIdamId(userId, caseReference).getId();

        UUID claimId = claimRepository.findIdByCaseReference(caseReference)
            .orElseThrow(() -> {
                log.error("No claim found for case: {}", caseReference);
                return new IllegalStateException(
                    String.format("No claim found for case: %d", caseReference));
            });

        PartyEntity partyRef = partyRepository.getReferenceById(partyId);
        ClaimEntity claimRef = claimRepository.getReferenceById(claimId);

        DefendantResponses responses = possessionClaimResponse.getDefendantResponses();

        DefendantResponseEntity responseEntity =
            buildDefendantResponseEntity(claimRef, partyRef, responses);

        buildAndLinkChildEntities(responseEntity, responses);

        DefendantResponseEntity savedResponse = defendantResponseRepository.save(responseEntity);

        if (responses != null && responses.getUploadedDocuments() != null) {
            documentService.createDefendantEvidenceDocuments(
                responses.getUploadedDocuments(),
                savedResponse
            );
        }

        log.info("Successfully saved defendant response for case {} user {}", caseReference, userId);
    }

    private DefendantResponseEntity buildDefendantResponseEntity(ClaimEntity claimRef,
                                                                PartyEntity partyRef,
                                                                DefendantResponses responses) {

        YesNoNotSure tenancyStartDateConfirmation = responses.getTenancyStartDateConfirmation();
        DefendantResponseEntity defendantResponse = DefendantResponseEntity.builder()
            .claim(claimRef)
            .party(partyRef)
            .freeLegalAdvice(responses.getFreeLegalAdvice())
            .possessionNoticeReceived(responses.getPossessionNoticeReceived())
            .defendantNameConfirmation(responses.getDefendantNameConfirmation())
            .landlordRegistered(responses.getLandlordRegistered())
            .writtenTerms(responses.getWrittenTerms())
            .disputeClaim(responses.getDisputeClaim())
            .disputeClaimDetails(responses.getDisputeClaimDetails())
            .tenancyStartDateConfirmation(tenancyStartDateConfirmation)
            .tenancyStartDate(
                responses.getTenancyStartDate() != null && tenancyStartDateConfirmation != YesNoNotSure.NOT_SURE
                    ? responses.getTenancyStartDate()
                    : null
            )
            .landlordLicensed(responses.getLandlordLicensed())
            .noticeReceivedDate(responses.getNoticeReceivedDate())
            .rentArrearsAmountConfirmation(responses.getRentArrearsAmountConfirmation())
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
}
