package uk.gov.hmcts.reform.pcs.enforcement.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.reform.pcs.enforcement.document.EnforcementDocumentScheduler;
import uk.gov.hmcts.reform.pcs.enforcement.domain.EnforcementDecision;
import uk.gov.hmcts.reform.pcs.enforcement.domain.EnforcementStatus;
import uk.gov.hmcts.reform.pcs.enforcement.domain.SelectEnforcementType;
import uk.gov.hmcts.reform.pcs.enforcement.entity.EnforcementOrderEntity;
import uk.gov.hmcts.reform.pcs.enforcement.exception.EnforcementEventNotAvailableException;
import uk.gov.hmcts.reform.pcs.enforcement.notification.EnforcementNotificationService;
import uk.gov.hmcts.reform.pcs.enforcement.repository.EnforcementOrderRepository;

import java.util.Optional;

import static uk.gov.hmcts.reform.pcs.enforcement.domain.EnforcementStatus.AWAITING_BAILIFF_ALLOCATION;
import static uk.gov.hmcts.reform.pcs.enforcement.domain.EnforcementStatus.AWAITING_REVIEW;
import static uk.gov.hmcts.reform.pcs.enforcement.domain.EnforcementStatus.REJECTED;
import static uk.gov.hmcts.reform.pcs.enforcement.domain.EnforcementStatus.TRANSFERRED_TO_HIGH_COURT;

/**
 * Records a caseworker's decision on the claim's enforcement application that is awaiting review.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EnforcementDecisionService {

    public static final String NO_REJECTION_REASON_MESSAGE =
        "A reason is needed to reject an enforcement application";

    private final EnforcementOrderRepository enforcementOrderRepository;
    private final CurrentEnforcementService currentEnforcementService;
    private final EnforcementStatusTransitions enforcementStatusTransitions;
    private final EnforcementNumberGenerator enforcementNumberGenerator;
    private final EnforcementNotificationService enforcementNotificationService;
    private final EnforcementDocumentScheduler enforcementDocumentScheduler;
    private final EnforcementWaTaskService enforcementWaTaskService;

    public boolean hasEnforcementAwaitingReview(long caseReference) {
        return findAwaitingReview(caseReference).isPresent();
    }

    /**
     * Approves the application: a warrant goes on to bailiff allocation, a writ is transferred to the High Court.
     * Gives it a warrant or writ number and records when it was approved.
     *
     * <p>Returns empty, rather than throwing, when the claim has no application awaiting review: the submit
     * handler runs inside the transaction the SDK opens for the event, so an exception out of this method would
     * mark that transaction rollback-only and the caller could not turn it into an error for the caseworker.</p>
     *
     * @return the outcome, or empty if no application is awaiting review
     */
    @Transactional
    public Optional<EnforcementDecisionOutcome> accept(long caseReference) {
        return findAwaitingReview(caseReference).map(enforcement -> {
            SelectEnforcementType enforcementType = enforcementTypeOf(enforcement);

            enforcementStatusTransitions.transition(enforcement, approvedStatus(enforcementType));
            enforcement.setApprovedAt(enforcement.getStatusUpdatedAt());
            String enforcementNumber = enforcementNumberGenerator.next(enforcementType);
            enforcement.setEnforcementNumber(enforcementNumber);

            // TODO HDPI-9566: generate the warrant or writ in Welsh when the claimant asked for Welsh documents.
            enforcementDocumentScheduler.scheduleApprovalDocument(enforcement.getId(), enforcementType);
            enforcementOrderRepository.save(enforcement);
            enforcementNotificationService.notifyClaimantOfApproval(caseReference, enforcementType, enforcementNumber);
            if (enforcement.getStatus() == AWAITING_BAILIFF_ALLOCATION) {
                // An approved writ goes to a High Court Enforcement Officer, so it gets no bailiff and no task.
                enforcementWaTaskService.warrantAwaitingBailiff(caseReference, enforcement.getId());
            }
            log.info("Enforcement {} on case {} approved as {}", enforcement.getId(), caseReference,
                     enforcementNumber);
            return new EnforcementDecisionOutcome(enforcementType, EnforcementDecision.ACCEPT, enforcementNumber);
        });
    }

    /**
     * Rejects the application. No number is given, and the claimant can apply again because a rejected
     * enforcement is not live.
     *
     * @return the outcome, or empty if no application is awaiting review
     * @throws EnforcementEventNotAvailableException if no rejection reason was given
     */
    @Transactional
    public Optional<EnforcementDecisionOutcome> reject(long caseReference, String rejectionReason) {
        if (StringUtils.isBlank(rejectionReason)) {
            throw new EnforcementEventNotAvailableException(NO_REJECTION_REASON_MESSAGE);
        }
        return findAwaitingReview(caseReference).map(enforcement -> {
            final SelectEnforcementType enforcementType = enforcementTypeOf(enforcement);

            enforcementStatusTransitions.transition(enforcement, REJECTED);
            enforcement.setRejectionReason(rejectionReason.trim());

            enforcementOrderRepository.save(enforcement);
            enforcementNotificationService.notifyClaimantOfRejection(
                caseReference, enforcementType, enforcement.getRejectionReason());
            log.info("Enforcement {} on case {} rejected", enforcement.getId(), caseReference);
            return new EnforcementDecisionOutcome(enforcementType, EnforcementDecision.REJECT, null);
        });
    }

    static EnforcementStatus approvedStatus(SelectEnforcementType enforcementType) {
        return switch (enforcementType) {
            case WARRANT, WARRANT_OF_RESTITUTION -> AWAITING_BAILIFF_ALLOCATION;
            case WRIT, WRIT_OF_RESTITUTION -> TRANSFERRED_TO_HIGH_COURT;
        };
    }

    /**
     * Whether a caseworker can decide the claim's application: the current enforcement is awaiting review. A
     * claim has at most one live enforcement, so at most one awaiting review, and the current one is the live
     * one if there is one. "Decide enforcement" is offered on this, and its start and submit refuse on it.
     *
     * @param currentEnforcement the claim's current enforcement, or null if it has none
     */
    public static boolean canDecideEnforcement(EnforcementOrderEntity currentEnforcement) {
        return currentEnforcement != null && currentEnforcement.getStatus() == AWAITING_REVIEW;
    }

    private Optional<EnforcementOrderEntity> findAwaitingReview(long caseReference) {
        return currentEnforcementService.findCurrentEnforcement(caseReference)
            .filter(EnforcementDecisionService::canDecideEnforcement);
    }

    private static SelectEnforcementType enforcementTypeOf(EnforcementOrderEntity enforcement) {
        SelectEnforcementType enforcementType = SelectEnforcementType.from(enforcement.getEnforcementOrder());
        if (enforcementType == null) {
            throw new IllegalStateException("Enforcement %s has no enforcement type".formatted(enforcement.getId()));
        }
        return enforcementType;
    }
}
