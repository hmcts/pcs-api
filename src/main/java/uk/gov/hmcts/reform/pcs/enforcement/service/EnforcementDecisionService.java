package uk.gov.hmcts.reform.pcs.enforcement.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.reform.pcs.enforcement.document.EnforcementDocumentScheduler;
import uk.gov.hmcts.reform.pcs.enforcement.domain.EnforcementDecision;
import uk.gov.hmcts.reform.pcs.enforcement.domain.EnforcementRejectionReason;
import uk.gov.hmcts.reform.pcs.enforcement.domain.EnforcementStatus;
import uk.gov.hmcts.reform.pcs.enforcement.domain.SelectEnforcementType;
import uk.gov.hmcts.reform.pcs.enforcement.entity.EnforcementOrderEntity;
import uk.gov.hmcts.reform.pcs.enforcement.exception.NoEnforcementAwaitingReviewException;
import uk.gov.hmcts.reform.pcs.enforcement.notification.EnforcementNotificationService;
import uk.gov.hmcts.reform.pcs.enforcement.repository.EnforcementOrderRepository;

import java.util.Optional;

import static uk.gov.hmcts.reform.pcs.enforcement.domain.EnforcementStatus.AWAITING_BAILIFF_ALLOCATION;
import static uk.gov.hmcts.reform.pcs.enforcement.domain.EnforcementStatus.AWAITING_REVIEW;
import static uk.gov.hmcts.reform.pcs.enforcement.domain.EnforcementStatus.REJECTED;
import static uk.gov.hmcts.reform.pcs.enforcement.domain.EnforcementStatus.TRANSFERRED_TO_HIGH_COURT;

@Slf4j
@Service
@RequiredArgsConstructor
public class EnforcementDecisionService {

    public static final String NO_REJECTION_REASON_SELECTED_MESSAGE =
        "A reason must be selected to reject an enforcement application";
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

    @Transactional
    public EnforcementChange<EnforcementDecisionOutcome> accept(long caseReference) {
        Optional<EnforcementOrderEntity> awaitingReview = findAwaitingReview(caseReference);
        if (awaitingReview.isEmpty()) {
            return EnforcementChange.refused(NoEnforcementAwaitingReviewException.MESSAGE);
        }
        EnforcementOrderEntity enforcement = awaitingReview.get();
        SelectEnforcementType enforcementType = enforcementTypeOf(enforcement);

        enforcementStatusTransitions.transition(enforcement, approvedStatus(enforcementType));
        enforcement.setApprovedAt(enforcement.getStatusUpdatedAt());
        String enforcementNumber = enforcementNumberGenerator.next(enforcementType);
        enforcement.setEnforcementNumber(enforcementNumber);
        enforcementOrderRepository.save(enforcement);

        EnforcementChange<Void> scheduled = EnforcementSideEffects.schedule(
            caseReference, "the approved warrant or writ, the claimant's email and the allocation task", () -> {
                // TODO HDPI-9566: generate the warrant or writ in Welsh when the claimant asked for Welsh
                //  documents.
                enforcementDocumentScheduler.scheduleApprovalDocument(enforcement.getId(), enforcementType);
                enforcementNotificationService.notifyClaimantOfApproval(caseReference, enforcementType,
                                                                        enforcementNumber);
                if (enforcement.getStatus() == AWAITING_BAILIFF_ALLOCATION) {
                    // An approved writ goes to a High Court Enforcement Officer, so gets no bailiff task.
                    enforcementWaTaskService.warrantAwaitingBailiff(caseReference, enforcement.getId());
                }
            });
        if (scheduled.refused()) {
            return EnforcementChange.refused(scheduled.errors());
        }
        log.info("Enforcement {} on case {} approved as {}", enforcement.getId(), caseReference, enforcementNumber);
        return EnforcementChange.made(
            new EnforcementDecisionOutcome(enforcementType, EnforcementDecision.ACCEPT, enforcementNumber));
    }

    @Transactional
    public EnforcementChange<EnforcementDecisionOutcome> reject(long caseReference,
                                                                EnforcementRejectionReason rejectionReason,
                                                                String rejectionReasonDetail) {
        if (rejectionReason == null) {
            return EnforcementChange.refused(NO_REJECTION_REASON_SELECTED_MESSAGE);
        }
        if (rejectionReason.isDetailRequired() && StringUtils.isBlank(rejectionReasonDetail)) {
            return EnforcementChange.refused(NO_REJECTION_REASON_MESSAGE);
        }
        Optional<EnforcementOrderEntity> awaitingReview = findAwaitingReview(caseReference);
        if (awaitingReview.isEmpty()) {
            return EnforcementChange.refused(NoEnforcementAwaitingReviewException.MESSAGE);
        }
        EnforcementOrderEntity enforcement = awaitingReview.get();
        final SelectEnforcementType enforcementType = enforcementTypeOf(enforcement);

        enforcementStatusTransitions.transition(enforcement, REJECTED);
        enforcement.setRejectionReasonCode(rejectionReason);
        // A reason that stands alone keeps no text, even where the caseworker typed some under another one
        // before changing their mind: the detail reaches the claimant's email and must match the reason.
        enforcement.setRejectionReason(rejectionReason.isDetailRequired()
                                           ? StringUtils.trimToNull(rejectionReasonDetail) : null);
        enforcementOrderRepository.save(enforcement);

        EnforcementChange<Void> scheduled = EnforcementSideEffects.schedule(
            caseReference, "the claimant's rejection email",
            () -> enforcementNotificationService.notifyClaimantOfRejection(
                caseReference, enforcementType, enforcement.getRejectionReason()));
        if (scheduled.refused()) {
            return EnforcementChange.refused(scheduled.errors());
        }
        log.info("Enforcement {} on case {} rejected", enforcement.getId(), caseReference);
        return EnforcementChange.made(
            new EnforcementDecisionOutcome(enforcementType, EnforcementDecision.REJECT, null));
    }

    static EnforcementStatus approvedStatus(SelectEnforcementType enforcementType) {
        return switch (enforcementType) {
            case WARRANT, WARRANT_OF_RESTITUTION -> AWAITING_BAILIFF_ALLOCATION;
            case WRIT, WRIT_OF_RESTITUTION -> TRANSFERRED_TO_HIGH_COURT;
        };
    }

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
