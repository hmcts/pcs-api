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

    public static final String NO_LIVE_EVICTION_ERROR =
        "This eviction is no longer active, so your answers have not been saved. "
            + "Refresh the case to see the latest enforcement status.";
    public static final String EVICTION_CHANGED_ERROR =
        "The eviction has changed since you started, for example it has been rescheduled or closed, so your "
            + "answers have not been saved. Refresh the case to see the latest eviction details.";

    private final CurrentEnforcementService currentEnforcementService;
    private final EnforcementOrderRepository enforcementOrderRepository;
    private final EnforcementStatusTransitions enforcementStatusTransitions;
    private final ClaimantRiskAssessmentService claimantRiskAssessmentService;
    private final EnforcementNotificationService enforcementNotificationService;
    private final EnforcementWaTaskService enforcementWaTaskService;

    /**
     * Saves any change to the claimant's risk assessment answers (HDPI-7552), then records whether they can
     * attend, in one transaction. A refusal from either is passed back for the submit handler to answer with,
     * which rolls the whole confirmation back with it.
     */
    @Transactional
    public EnforcementChange<Void> recordConfirmation(long caseReference, YesOrNo canAttend,
                                                      EnforcementOrder submitted) {
        EnforcementChange<Boolean> riskAnswers = claimantRiskAssessmentService.recordAnswers(caseReference, submitted);
        if (riskAnswers.refused()) {
            return EnforcementChange.refused(riskAnswers.errors());
        }
        return recordAttendance(caseReference, canAttend);
    }

    /**
     * Moves the live enforcement to "Claimant confirmed" or "Claimant unable to attend". This only happens
     * when it is awaiting the claimant's confirmation, which "Schedule eviction" and "Reschedule eviction" move it
     * to (HDPI-7344, HDPI-7549). Otherwise another user has moved the warrant on while the journey was open, so
     * the answer is refused and the claimant told to refresh the case, rather than appearing to be recorded.
     *
     * <p>The 72-hour confirmation deadline the start page states is not enforced here.</p>
     */
    @Transactional
    public EnforcementChange<Void> recordAttendance(long caseReference, YesOrNo canAttend) {
        if (canAttend == null) {
            log.info("Case {}: no attendance answer, enforcement status not changed", caseReference);
            return EnforcementChange.made();
        }
        Optional<EnforcementOrderEntity> liveEnforcement = findLiveEnforcement(caseReference);
        if (liveEnforcement.isEmpty()) {
            log.info("Case {}: no live enforcement, attendance answer refused", caseReference);
            return EnforcementChange.refused(NO_LIVE_EVICTION_ERROR);
        }
        EnforcementOrderEntity enforcement = liveEnforcement.get();
        if (enforcement.getStatus() != AWAITING_CLAIMANT_CONFIRMATION) {
            log.info("Case {}: enforcement {} is {}, not {}, so the attendance answer is refused",
                     caseReference, enforcement.getId(), enforcement.getStatus(), AWAITING_CLAIMANT_CONFIRMATION);
            return EnforcementChange.refused(EVICTION_CHANGED_ERROR);
        }
        EnforcementStatus target = canAttend.toBoolean() ? CLAIMANT_CONFIRMED : CLAIMANT_UNABLE_TO_ATTEND;
        enforcementStatusTransitions.transition(enforcement, target);
        enforcement.setClaimantCanAttend(canAttend.toBoolean());
        enforcementOrderRepository.save(enforcement);
        // TODO HDPI-7549: store the dates the claimant is not available, which the journey does not ask for yet,
        // so the bailiff rescheduling the eviction can avoid them.
        return EnforcementSideEffects.schedule(
            caseReference, "the claimant's attendance email and the reschedule task", () -> {
                if (target == CLAIMANT_CONFIRMED) {
                    enforcementNotificationService.notifyClaimantOfConfirmedAttendance(
                        caseReference, enforcement.getBailiffDate());
                } else {
                    enforcementNotificationService.notifyClaimantOfDeclinedAttendance(caseReference);
                    enforcementWaTaskService.claimantCannotAttend(caseReference, enforcement.getId(),
                                                                 enforcement.getBailiffDate());
                }
            });
    }

    private Optional<EnforcementOrderEntity> findLiveEnforcement(long caseReference) {
        // A claim can have at most one live enforcement, and it is always the current one.
        return currentEnforcementService.findCurrentEnforcement(caseReference)
            .filter(enforcement -> enforcement.getStatus().isLive());
    }
}
