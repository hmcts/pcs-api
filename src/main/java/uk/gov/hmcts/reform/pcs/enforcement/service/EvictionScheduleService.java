package uk.gov.hmcts.reform.pcs.enforcement.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.enforcement.domain.EnforcementStatus;
import uk.gov.hmcts.reform.pcs.enforcement.entity.EnforcementOrderEntity;
import uk.gov.hmcts.reform.pcs.enforcement.exception.EnforcementEventNotAvailableException;
import uk.gov.hmcts.reform.pcs.enforcement.exception.NoEnforcementException;
import uk.gov.hmcts.reform.pcs.enforcement.repository.EnforcementOrderRepository;

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

/**
 * The bailiff schedules and reschedules the eviction on the claim's live warrant, and marks the notice of
 * eviction as served (HDPI-7344, HDPI-7549, HDPI-7346).
 *
 * <p>The eviction date is the enforcement's bailiff date, which also drives the claimant's Confirm eviction
 * journey. Scheduling and rescheduling move the warrant to awaiting claimant confirmation. Marking the notice as
 * served does not change the status.</p>
 */
@Service
@RequiredArgsConstructor
public class EvictionScheduleService {

    public static final String CANNOT_SCHEDULE_ERROR = "The eviction cannot be scheduled: the warrant is not "
        + "awaiting scheduling";
    public static final String CANNOT_RESCHEDULE_ERROR = "The eviction cannot be rescheduled until the claimant "
        + "has said whether they can attend";
    public static final String CANNOT_MARK_NOTICE_SERVED_ERROR = "There is no 'notice of eviction' to mark as "
        + "served on this warrant";

    /**
     * The statuses an eviction can be rescheduled from: the claimant cannot attend, or has confirmed. Claimant
     * confirmed comes from the event availability rules (HDPI-7549, open question 13).
     */
    static final Set<EnforcementStatus> RESCHEDULABLE = Collections.unmodifiableSet(
        EnumSet.of(CLAIMANT_UNABLE_TO_ATTEND, CLAIMANT_CONFIRMED));

    /**
     * The statuses of a warrant with a scheduled eviction that has not happened yet.
     */
    static final Set<EnforcementStatus> SCHEDULED = Collections.unmodifiableSet(
        EnumSet.of(AWAITING_CLAIMANT_CONFIRMATION, CLAIMANT_CONFIRMED, CLAIMANT_UNABLE_TO_ATTEND));

    private final CurrentEnforcementService currentEnforcementService;
    private final EnforcementOrderRepository enforcementOrderRepository;
    private final EnforcementStatusTransitions enforcementStatusTransitions;
    private final BailiffNoteService bailiffNoteService;

    public static boolean canSchedule(EnforcementOrderEntity enforcement) {
        return enforcement.getStatus() == AWAITING_SCHEDULING;
    }

    public static boolean canReschedule(EnforcementOrderEntity enforcement) {
        return enforcement.getStatus() != null && RESCHEDULABLE.contains(enforcement.getStatus());
    }

    /**
     * The notice of eviction is to be served when the eviction is scheduled, the court has not dispensed with
     * service, and the notice has not been served yet.
     */
    public static boolean canMarkNoticeServed(EnforcementOrderEntity enforcement) {
        return enforcement.getStatus() != null
            && SCHEDULED.contains(enforcement.getStatus())
            && enforcement.getBailiffDate() != null
            && Boolean.FALSE.equals(enforcement.getDispenseWithService())
            && enforcement.getNoticeServedAt() == null;
    }

    /**
     * Defendants may see the enforcement once the notice of eviction is served, and never when the court has
     * dispensed with service (a without notice eviction).
     */
    public static boolean defendantsCanSeeEnforcement(EnforcementOrderEntity enforcement) {
        return !Boolean.TRUE.equals(enforcement.getDispenseWithService()) && enforcement.getNoticeServedAt() != null;
    }

    public Optional<EnforcementOrderEntity> findCurrentEnforcement(PcsCaseEntity pcsCaseEntity) {
        return currentEnforcementService.findCurrentEnforcement(pcsCaseEntity);
    }

    /**
     * Checks the eviction can be scheduled on the claim's live warrant.
     *
     * @throws NoEnforcementException if the claim has no live enforcement
     * @throws EnforcementEventNotAvailableException if the warrant is not awaiting scheduling
     */
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

    /**
     * Saves the eviction date on the claim's live warrant.
     *
     * @return the warrant's number for the confirmation screen, or the errors to show instead
     */
    @Transactional
    public EnforcementChange<String> schedule(long caseReference, LocalDateTime evictionDateTime,
                                              boolean dispenseWithService) {
        EnforcementChange<EnforcementOrderEntity> live = findLiveEnforcement(
            caseReference, EvictionScheduleService::canSchedule, CANNOT_SCHEDULE_ERROR);
        if (live.refused()) {
            return EnforcementChange.refused(live.errors());
        }
        setEvictionDate(live.change(), evictionDateTime, dispenseWithService);
        // TODO HDPI-7354 / HDPI-8442: generate the sealed notice of eviction (form N54) into the Enforcement
        //  folder, internal only. There is no Docmosis template for it in pcs-api yet.
        // TODO HDPI-7355 / HDPI-8442: generate the unsealed notice of appointment into the Enforcement folder,
        //  visible to the claimant, and email it to them. There is no Docmosis template for it yet.
        // TODO HDPI-7538: send the claimant the "Bailiff schedules the eviction date" email and the "Confirm the
        //  eviction date" dashboard notification. The Notify templates do not exist yet.
        // TODO HDPI-7346 / HDPI-8627: create the "Mark notice of eviction as served" WA task for the allocated
        //  bailiff when service is not dispensed with.
        // TODO HDPI-7347: create the "Record warrant return" WA task due on the eviction date.
        return EnforcementChange.made(live.change().getEnforcementNumber());
    }

    /**
     * Saves a new eviction date on the claim's live warrant.
     *
     * @return the warrant's number for the confirmation screen, or the errors to show instead
     */
    @Transactional
    public EnforcementChange<String> reschedule(long caseReference, LocalDateTime evictionDateTime,
                                                boolean dispenseWithService) {
        EnforcementChange<EnforcementOrderEntity> live = findLiveEnforcement(
            caseReference, EvictionScheduleService::canReschedule, CANNOT_RESCHEDULE_ERROR);
        if (live.refused()) {
            return EnforcementChange.refused(live.errors());
        }
        setEvictionDate(live.change(), evictionDateTime, dispenseWithService);
        // TODO HDPI-7354 / HDPI-7355: generate a new notice of eviction and notice of appointment, keeping the
        //  originals in the Enforcement folder. There are no Docmosis templates for them yet.
        // TODO HDPI-7538: send the claimant the "Bailiff reschedules the eviction date" email and the "Confirm
        //  the eviction date" dashboard notification. The Notify templates do not exist yet.
        // TODO HDPI-7549: complete the "Reschedule eviction" WA task, and move the "Record warrant return" task
        //  (HDPI-7347) to the new eviction date.
        return EnforcementChange.made(live.change().getEnforcementNumber());
    }

    /**
     * Records when and how the notice of eviction was served, and adds the bailiff note if there is one.
     *
     * @param bailiffNote the note to add, or null for none
     * @return the warrant's number for the confirmation screen, or the errors to show instead
     */
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
        // TODO HDPI-7346: complete the "Mark notice of eviction as served" WA task.
        // TODO HDPI-7539 / HDPI-8442 / HDPI-7354: let defendants see the enforcement status tab, the Enforcement
        //  folder and the notice of eviction, driven from defendantsCanSeeEnforcement.
        return EnforcementChange.made(enforcement.getEnforcementNumber());
    }

    /**
     * A new eviction date comes with a fresh notice of eviction (HDPI-7354), so the service record of the
     * previous notice no longer applies: clearing it makes "Mark eviction notice served" available again, and
     * takes defendant visibility away until the new notice is served. Defendant visibility is derived from the
     * notice fields, so it is recomputed after they are cleared. The claimant's answer was about the previous date, so
     * it is cleared too.
     */
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

    /**
     * The claim's live enforcement, for a start callback: it refuses by throwing, which
     * {@link uk.gov.hmcts.reform.pcs.enforcement.exception.EnforcementCallbackExceptionHandler} turns into an
     * error on the start response. A submit handler must go through {@link #findLiveEnforcement} instead.
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

    /**
     * The claim's live enforcement if the event applies to it, or why it does not.
     */
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
