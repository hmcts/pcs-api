package uk.gov.hmcts.reform.pcs.enforcement.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.enforcement.document.EnforcementDocumentScheduler;
import uk.gov.hmcts.reform.pcs.enforcement.domain.EnforcementStatus;
import uk.gov.hmcts.reform.pcs.enforcement.entity.EnforcementOrderEntity;
import uk.gov.hmcts.reform.pcs.enforcement.exception.EnforcementEventNotAvailableException;
import uk.gov.hmcts.reform.pcs.enforcement.exception.NoEnforcementException;
import uk.gov.hmcts.reform.pcs.enforcement.notification.EnforcementNotificationService;
import uk.gov.hmcts.reform.pcs.enforcement.repository.EnforcementOrderRepository;
import uk.gov.hmcts.reform.pcs.enforcement.service.EnforcementWaTaskService.Eviction;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;

import static uk.gov.hmcts.reform.pcs.enforcement.domain.EnforcementStatus.AWAITING_CLAIMANT_CONFIRMATION;
import static uk.gov.hmcts.reform.pcs.enforcement.domain.EnforcementStatus.AWAITING_SCHEDULING;
import static uk.gov.hmcts.reform.pcs.enforcement.domain.EnforcementStatus.CLAIMANT_CONFIRMED;
import static uk.gov.hmcts.reform.pcs.enforcement.domain.EnforcementStatus.CLAIMANT_UNABLE_TO_ATTEND;

@Service
@RequiredArgsConstructor
public class EvictionScheduleService {

    public static final String CANNOT_SCHEDULE_ERROR = "The eviction cannot be scheduled: the warrant is not "
        + "awaiting scheduling";
    public static final String CANNOT_RESCHEDULE_ERROR = "The eviction cannot be rescheduled until the claimant "
        + "has said whether they can attend";
    public static final String CANNOT_MARK_NOTICE_SERVED_ERROR = "There is no 'notice of eviction' to mark as "
        + "served on this warrant";

    static final Set<EnforcementStatus> RESCHEDULABLE = Collections.unmodifiableSet(
        EnumSet.of(CLAIMANT_UNABLE_TO_ATTEND, CLAIMANT_CONFIRMED));

    static final Set<EnforcementStatus> SCHEDULED = Collections.unmodifiableSet(
        EnumSet.of(AWAITING_CLAIMANT_CONFIRMATION, CLAIMANT_CONFIRMED, CLAIMANT_UNABLE_TO_ATTEND));

    private final CurrentEnforcementService currentEnforcementService;
    private final EnforcementOrderRepository enforcementOrderRepository;
    private final EnforcementStatusTransitions enforcementStatusTransitions;
    private final BailiffNoteService bailiffNoteService;
    private final EnforcementNotificationService enforcementNotificationService;
    private final EnforcementDocumentScheduler enforcementDocumentScheduler;
    private final EnforcementWaTaskService enforcementWaTaskService;

    public static boolean canSchedule(EnforcementOrderEntity enforcement) {
        return enforcement != null && enforcement.getStatus() == AWAITING_SCHEDULING;
    }

    public static boolean canReschedule(EnforcementOrderEntity enforcement) {
        return enforcement != null && enforcement.getStatus() != null
            && RESCHEDULABLE.contains(enforcement.getStatus());
    }

    public static boolean canMarkNoticeServed(EnforcementOrderEntity enforcement) {
        return enforcement != null
            && enforcement.getStatus() != null
            && SCHEDULED.contains(enforcement.getStatus())
            && enforcement.getBailiffDate() != null
            && Boolean.FALSE.equals(enforcement.getDispenseWithService())
            && enforcement.getNoticeServedAt() == null;
    }

    public static boolean defendantsCanSeeEnforcement(EnforcementOrderEntity enforcement) {
        return !Boolean.TRUE.equals(enforcement.getDispenseWithService()) && enforcement.getNoticeServedAt() != null;
    }

    public Optional<EnforcementOrderEntity> findCurrentEnforcement(PcsCaseEntity pcsCaseEntity) {
        return currentEnforcementService.findCurrentEnforcement(pcsCaseEntity);
    }

    public void checkCanSchedule(long caseReference) {
        requireLiveEnforcement(caseReference, EvictionScheduleService::canSchedule, CANNOT_SCHEDULE_ERROR);
    }

    public void checkCanReschedule(long caseReference) {
        requireLiveEnforcement(caseReference, EvictionScheduleService::canReschedule, CANNOT_RESCHEDULE_ERROR);
    }

    public void checkCanMarkNoticeServed(long caseReference) {
        requireLiveEnforcement(caseReference, EvictionScheduleService::canMarkNoticeServed,
                               CANNOT_MARK_NOTICE_SERVED_ERROR);
    }

    @Transactional
    public EnforcementChange<String> schedule(long caseReference, LocalDateTime evictionDateTime,
                                              boolean dispenseWithService) {
        EnforcementChange<EnforcementOrderEntity> live = findLiveEnforcement(
            caseReference, EvictionScheduleService::canSchedule, CANNOT_SCHEDULE_ERROR);
        if (live.refused()) {
            return EnforcementChange.refused(live.errors());
        }
        EnforcementOrderEntity enforcement = live.change();
        setEvictionDate(enforcement, evictionDateTime, dispenseWithService);
        final Eviction appointment = appointmentOf(enforcement);
        // TODO HDPI-7538: show the claimant the "Confirm the eviction date" dashboard notification.
        EnforcementChange<Void> scheduled = EnforcementSideEffects.schedule(
            caseReference, "the eviction notices, the claimant's email and the eviction tasks", () -> {
                enforcementDocumentScheduler.scheduleEvictionNotices(enforcement.getId());
                // TODO HDPI-7355: email the notice of appointment to the claimant.
                enforcementNotificationService.notifyClaimantOfScheduledEviction(caseReference, evictionDateTime);
                enforcementWaTaskService.evictionScheduled(caseReference, enforcement.getId(), appointment);
            });
        if (scheduled.refused()) {
            return EnforcementChange.refused(scheduled.errors());
        }
        return EnforcementChange.made(enforcement.getEnforcementNumber());
    }

    @Transactional
    public EnforcementChange<String> reschedule(long caseReference, LocalDateTime evictionDateTime,
                                                boolean dispenseWithService) {
        EnforcementChange<EnforcementOrderEntity> live = findLiveEnforcement(
            caseReference, EvictionScheduleService::canReschedule, CANNOT_RESCHEDULE_ERROR);
        if (live.refused()) {
            return EnforcementChange.refused(live.errors());
        }
        EnforcementOrderEntity enforcement = live.change();
        // Read before the new date overwrites it, so the previous appointment's tasks can be cancelled.
        final Eviction previous = appointmentOf(enforcement);
        setEvictionDate(enforcement, evictionDateTime, dispenseWithService);
        final Eviction appointment = appointmentOf(enforcement);
        // TODO HDPI-7538: show the claimant the "Confirm the eviction date" dashboard notification.
        EnforcementChange<Void> scheduled = EnforcementSideEffects.schedule(
            caseReference, "the new eviction notices, the claimant's email and the eviction tasks", () -> {
                enforcementDocumentScheduler.scheduleEvictionNotices(enforcement.getId());
                enforcementNotificationService.notifyClaimantOfRescheduledEviction(caseReference, evictionDateTime);
                enforcementWaTaskService.evictionRescheduled(caseReference, enforcement.getId(), previous,
                                                             appointment);
            });
        if (scheduled.refused()) {
            return EnforcementChange.refused(scheduled.errors());
        }
        return EnforcementChange.made(enforcement.getEnforcementNumber());
    }

    @Transactional
    public EnforcementChange<String> markNoticeServed(long caseReference, LocalDateTime servedAt, String servedHow,
                                                      String bailiffNote) {
        EnforcementChange<EnforcementOrderEntity> live = findLiveEnforcement(
            caseReference, EvictionScheduleService::canMarkNoticeServed, CANNOT_MARK_NOTICE_SERVED_ERROR);
        if (live.refused()) {
            return EnforcementChange.refused(live.errors());
        }
        EnforcementOrderEntity enforcement = live.change();
        enforcement.setNoticeServedAt(servedAt);
        enforcement.setNoticeServedHow(servedHow);
        enforcement.setDefendantsCanSeeEnforcement(defendantsCanSeeEnforcement(enforcement));
        enforcementOrderRepository.save(enforcement);
        if (bailiffNote != null) {
            bailiffNoteService.addNote(caseReference, bailiffNote);
        }
        return EnforcementChange.made(enforcement.getEnforcementNumber());
    }

    private void setEvictionDate(EnforcementOrderEntity enforcement, LocalDateTime evictionDateTime,
                                 boolean dispenseWithService) {
        enforcement.setBailiffDate(evictionDateTime);
        enforcement.setDispenseWithService(dispenseWithService);
        enforcement.setNoticeServedAt(null);
        enforcement.setNoticeServedHow(null);
        enforcement.setClaimantCanAttend(null);
        enforcement.setDefendantsCanSeeEnforcement(defendantsCanSeeEnforcement(enforcement));
        enforcementStatusTransitions.transition(enforcement, AWAITING_CLAIMANT_CONFIRMATION);
        enforcementOrderRepository.save(enforcement);
    }

    private static Eviction appointmentOf(EnforcementOrderEntity enforcement) {
        return new Eviction(enforcement.getBailiffDate(),
                            Boolean.TRUE.equals(enforcement.getDispenseWithService()),
                            enforcement.getNoticeServedAt() != null);
    }

    /**
     * Refuses by throwing, for a start callback. A submit handler must use {@link #findLiveEnforcement}.
     */
    private EnforcementOrderEntity requireLiveEnforcement(long caseReference,
                                                          Predicate<EnforcementOrderEntity> available,
                                                          String notAvailableError) {
        EnforcementOrderEntity enforcement = findLive(caseReference)
            .orElseThrow(NoEnforcementException::new);
        if (!available.test(enforcement)) {
            throw new EnforcementEventNotAvailableException(notAvailableError);
        }
        return enforcement;
    }

    private EnforcementChange<EnforcementOrderEntity> findLiveEnforcement(
        long caseReference, Predicate<EnforcementOrderEntity> available, String notAvailableError) {
        Optional<EnforcementOrderEntity> live = findLive(caseReference);
        if (live.isEmpty()) {
            return EnforcementChange.refused(NoEnforcementException.MESSAGE);
        }
        if (!available.test(live.get())) {
            return EnforcementChange.refused(notAvailableError);
        }
        return EnforcementChange.made(live.get());
    }

    private Optional<EnforcementOrderEntity> findLive(long caseReference) {
        return currentEnforcementService.findCurrentEnforcement(caseReference)
            .filter(current -> current.getStatus() != null && current.getStatus().isLive());
    }
}
