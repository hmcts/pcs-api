package uk.gov.hmcts.reform.pcs.ccd.service.respondpossessionclaim;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.respondpossessionclaim.DefendantResponses;
import uk.gov.hmcts.reform.pcs.ccd.domain.respondpossessionclaim.PossessionClaimResponse;
import uk.gov.hmcts.reform.pcs.ccd.domain.statementoftruth.StatementOfTruthCompletedBy;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.claim.StatementOfTruthEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.respondpossessionclaim.DefendantResponseEntity;
import uk.gov.hmcts.reform.pcs.ccd.repository.ClaimRepository;
import uk.gov.hmcts.reform.pcs.ccd.repository.DefendantResponseRepository;
import uk.gov.hmcts.reform.pcs.ccd.repository.PartyRepository;
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

    /**
     * Saves defendant's responses to the defendant_response table.
     *
     * <p>Uses optimized approach with minimal locking:
     * <ol>
     *   <li>Check duplicate first (fail fast)</li>
     *   <li>Get IDs only (minimal lock time)</li>
     *   <li>Use getReferenceById() for proxies (no query)</li>
     *   <li>Save - only locks new row being inserted</li>
     * </ol>
     *
     * <p>This approach ensures concurrent defendants can submit simultaneously
     * without blocking each other or other case operations.
     *
     * @param caseReference The case reference number
     * @param possessionClaimResponse responses from draft data
     * @throws IllegalStateException if user ID is null, response already exists,
     *         party not found, or claim not found
     */
    public void saveDefendantResponse(long caseReference, PossessionClaimResponse possessionClaimResponse) {
        UUID userId = securityContextService.getCurrentUserId();

        // Early return if no responses to save
        if (possessionClaimResponse == null) {
            log.debug("No defendant responses to save for case {}", caseReference);
            return;
        }

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
        DefendantResponseEntity defendantResponse = DefendantResponseEntity.builder()
            .claim(claimRef)
            .party(partyRef)
            .receivedFreeLegalAdvice(responses.getReceivedFreeLegalAdvice())
            .build();

        //set bidirectional relationship with the pcs case
        claimRef.getPcsCase().addDefendantResponse(defendantResponse);
        //empty for now to satisfy not null constraint
        defendantResponse.setStatementOfTruth(StatementOfTruthEntity.builder()
                                                   .completedBy(StatementOfTruthCompletedBy.LEGAL_REPRESENTATIVE)
                                                   .accepted(YesOrNo.YES)
                                                   .fullName("John test")
                                                   .build());

        buildAndLinkChildEntities(defendantResponse, possessionClaimResponse);

        defendantResponseRepository.save(defendantResponse);

        log.info("Successfully saved defendant response for case {} user {}", caseReference, userId);
    }


    public void buildAndLinkChildEntities(
        DefendantResponseEntity defendantResponseEntity,
        PossessionClaimResponse response) {

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
