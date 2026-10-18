package uk.gov.hmcts.reform.pcs.enforcement.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.enforcement.domain.EnforcementOrder;
import uk.gov.hmcts.reform.pcs.enforcement.domain.EnforcementStatus;
import uk.gov.hmcts.reform.pcs.enforcement.entity.EnforcementOrderEntity;
import uk.gov.hmcts.reform.pcs.enforcement.notification.EnforcementNotificationService;
import uk.gov.hmcts.reform.pcs.enforcement.repository.EnforcementOrderRepository;

import java.util.Optional;

import static uk.gov.hmcts.reform.pcs.enforcement.domain.EnforcementStatus.AWAITING_CLAIMANT_CONFIRMATION;
import static uk.gov.hmcts.reform.pcs.enforcement.domain.EnforcementStatus.CLAIMANT_CONFIRMED;
import static uk.gov.hmcts.reform.pcs.enforcement.domain.EnforcementStatus.CLAIMANT_UNABLE_TO_ATTEND;

/**
 * Records the claimant's answers to "Confirm the eviction details" against the claim's live enforcement.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ClaimantEvictionConfirmationService {

    private final CurrentEnforcementService currentEnforcementService;
    private final EnforcementOrderRepository enforcementOrderRepository;
    private final EnforcementStatusTransitions enforcementStatusTransitions;
    private final ClaimantRiskAssessmentService claimantRiskAssessmentService;
    private final EnforcementNotificationService enforcementNotificationService;

    /**
     * Saves any change to the claimant's risk assessment answers (HDPI-7552), then records whether they can
     * attend, in one transaction.
     */
    @Transactional
    public void recordConfirmation(long caseReference, YesOrNo canAttend, EnforcementOrder submitted) {
        claimantRiskAssessmentService.recordAnswers(caseReference, submitted);
        recordAttendance(caseReference, canAttend);
    }

    /**
     * Moves the live enforcement to "Claimant confirmed" or "Claimant unable to attend". This only happens
     * when it is awaiting the claimant's confirmation, which "Schedule eviction" and "Reschedule eviction" move it
     * to (HDPI-7344, HDPI-7549). In any other case the status is left as it is: another user may have moved the
     * warrant on while the journey was open.
     */
    @Transactional
    public void recordAttendance(long caseReference, YesOrNo canAttend) {
        if (canAttend == null) {
            log.info("Case {}: no attendance answer, enforcement status not changed", caseReference);
            return;
        }
        Optional<EnforcementOrderEntity> liveEnforcement = findLiveEnforcement(caseReference);
        if (liveEnforcement.isEmpty()) {
            log.info("Case {}: no live enforcement, enforcement status not changed", caseReference);
            return;
        }
        EnforcementOrderEntity enforcement = liveEnforcement.get();
        if (enforcement.getStatus() != AWAITING_CLAIMANT_CONFIRMATION) {
            log.info("Case {}: enforcement {} is {}, not {}, so its status is not changed",
                     caseReference, enforcement.getId(), enforcement.getStatus(), AWAITING_CLAIMANT_CONFIRMATION);
            return;
        }
        EnforcementStatus target = canAttend.toBoolean() ? CLAIMANT_CONFIRMED : CLAIMANT_UNABLE_TO_ATTEND;
        enforcementStatusTransitions.transition(enforcement, target);
        enforcementOrderRepository.save(enforcement);
        if (target == CLAIMANT_CONFIRMED) {
            enforcementNotificationService.notifyClaimantOfConfirmedAttendance(
                caseReference, enforcement.getBailiffDate());
        } else {
            enforcementNotificationService.notifyClaimantOfDeclinedAttendance(caseReference);
        }
        // TODO HDPI-7549: when the claimant cannot attend, create a "Reschedule eviction" task for the allocated
        // bailiff, and store the dates they are not available (the journey does not ask for them yet). It needs
        // Work Allocation.
    }

    private Optional<EnforcementOrderEntity> findLiveEnforcement(long caseReference) {
        // A claim can have at most one live enforcement, and it is always the current one.
        return currentEnforcementService.findCurrentEnforcement(caseReference)
            .filter(enforcement -> enforcement.getStatus().isLive());
    }
}
