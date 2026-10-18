package uk.gov.hmcts.reform.pcs.enforcement.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.pcs.enforcement.domain.EnforcementStatus;
import uk.gov.hmcts.reform.pcs.enforcement.entity.EnforcementOrderEntity;
import uk.gov.hmcts.reform.pcs.enforcement.exception.IllegalEnforcementStatusTransitionException;

import java.time.Clock;
import java.time.Instant;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

import static uk.gov.hmcts.reform.pcs.enforcement.domain.EnforcementStatus.AWAITING_BAILIFF_ALLOCATION;
import static uk.gov.hmcts.reform.pcs.enforcement.domain.EnforcementStatus.AWAITING_CLAIMANT_CONFIRMATION;
import static uk.gov.hmcts.reform.pcs.enforcement.domain.EnforcementStatus.AWAITING_PAYMENT;
import static uk.gov.hmcts.reform.pcs.enforcement.domain.EnforcementStatus.AWAITING_PROPERTY_VISIT;
import static uk.gov.hmcts.reform.pcs.enforcement.domain.EnforcementStatus.AWAITING_REVIEW;
import static uk.gov.hmcts.reform.pcs.enforcement.domain.EnforcementStatus.AWAITING_SCHEDULING;
import static uk.gov.hmcts.reform.pcs.enforcement.domain.EnforcementStatus.BREATHING_SPACE;
import static uk.gov.hmcts.reform.pcs.enforcement.domain.EnforcementStatus.CLAIMANT_CONFIRMED;
import static uk.gov.hmcts.reform.pcs.enforcement.domain.EnforcementStatus.CLAIMANT_UNABLE_TO_ATTEND;
import static uk.gov.hmcts.reform.pcs.enforcement.domain.EnforcementStatus.CLOSED;
import static uk.gov.hmcts.reform.pcs.enforcement.domain.EnforcementStatus.REJECTED;
import static uk.gov.hmcts.reform.pcs.enforcement.domain.EnforcementStatus.TRANSFERRED_TO_HIGH_COURT;

/**
 * The enforcement status state machine. Every status change goes through {@link #transition}, which
 * rejects any move not in the transition table of 19-enforcement-status-model.feature, or in the warrant
 * return additions from 11-record-warrant-return.feature.
 */
@Component
@RequiredArgsConstructor
public class EnforcementStatusTransitions {

    /**
     * The statuses a new enforcement can start in: awaiting payment, or awaiting review when no fee is due.
     */
    static final Set<EnforcementStatus> INITIAL = Collections.unmodifiableSet(
        EnumSet.of(AWAITING_PAYMENT, AWAITING_REVIEW));

    private static final Map<EnforcementStatus, Set<EnforcementStatus>> ALLOWED = allowedTransitions();

    private final Clock utcClock;

    /**
     * Moves the enforcement to {@code target} and records when. An enforcement with no status yet can only
     * move to one of the initial statuses.
     *
     * @throws IllegalEnforcementStatusTransitionException if the move is not allowed
     */
    public void transition(EnforcementOrderEntity enforcement, EnforcementStatus target) {
        EnforcementStatus current = enforcement.getStatus();
        if (!isAllowed(current, target)) {
            throw new IllegalEnforcementStatusTransitionException(enforcement.getId(), current, target);
        }
        enforcement.setStatus(target);
        enforcement.setStatusUpdatedAt(Instant.now(utcClock));
    }

    public static boolean isAllowed(EnforcementStatus from, EnforcementStatus to) {
        return to != null && allowedTargets(from).contains(to);
    }

    public static Set<EnforcementStatus> allowedTargets(EnforcementStatus from) {
        return from == null ? INITIAL : ALLOWED.get(from);
    }

    /**
     * The status a newly submitted application starts in.
     */
    public static EnforcementStatus initialStatus(boolean paymentOutstanding) {
        return paymentOutstanding ? AWAITING_PAYMENT : AWAITING_REVIEW;
    }

    private static Map<EnforcementStatus, Set<EnforcementStatus>> allowedTransitions() {
        Map<EnforcementStatus, Set<EnforcementStatus>> allowed = new EnumMap<>(EnforcementStatus.class);
        for (EnforcementStatus status : EnforcementStatus.values()) {
            allowed.put(status, EnumSet.noneOf(EnforcementStatus.class));
        }
        // the claimant pays
        allow(allowed, AWAITING_PAYMENT, AWAITING_REVIEW);
        // the caseworker approves a warrant, rejects the application, or approves a writ
        allow(allowed, AWAITING_REVIEW, AWAITING_BAILIFF_ALLOCATION, REJECTED, TRANSFERRED_TO_HIGH_COURT);
        // a bailiff is allocated
        allow(allowed, AWAITING_BAILIFF_ALLOCATION, AWAITING_SCHEDULING);
        // the bailiff schedules the eviction
        allow(allowed, AWAITING_SCHEDULING, AWAITING_CLAIMANT_CONFIRMATION);
        // the claimant confirms they can, or cannot, attend
        allow(allowed, AWAITING_CLAIMANT_CONFIRMATION, CLAIMANT_CONFIRMED, CLAIMANT_UNABLE_TO_ATTEND);
        // the bailiff reschedules the eviction
        allow(allowed, CLAIMANT_UNABLE_TO_ATTEND, AWAITING_CLAIMANT_CONFIRMATION);
        // the bailiff records a closing final, breathing space or obstruction return
        allow(allowed, CLAIMANT_CONFIRMED, CLOSED, BREATHING_SPACE, AWAITING_SCHEDULING);
        // the bailiff reschedules an eviction the claimant has already confirmed. Not in the feature's
        // transition table, but its event availability rules offer "Reschedule eviction" in Claimant confirmed
        // (HDPI-7549, OPEN-QUESTIONS 7).
        allow(allowed, CLAIMANT_CONFIRMED, AWAITING_CLAIMANT_CONFIRMATION);
        // the bailiff records "Reopen warrant"
        allow(allowed, CLOSED, AWAITING_BAILIFF_ALLOCATION);
        // 11-record-warrant-return.feature (HDPI-8424, "Return name moves the enforcement status"): a return can
        // be recorded at any point after a bailiff is allocated, not only once the claimant has confirmed, and
        // closes the warrant, puts it in breathing space or sends it back for scheduling. A warrant in breathing
        // space can still take a return (19-enforcement-status-model.feature event availability), which is how
        // it leaves breathing space (OPEN-QUESTIONS 4).
        allow(allowed, AWAITING_SCHEDULING, CLOSED, BREATHING_SPACE);
        allow(allowed, AWAITING_CLAIMANT_CONFIRMATION, CLOSED, BREATHING_SPACE, AWAITING_SCHEDULING);
        allow(allowed, CLAIMANT_UNABLE_TO_ATTEND, CLOSED, BREATHING_SPACE, AWAITING_SCHEDULING);
        allow(allowed, AWAITING_PROPERTY_VISIT, CLOSED, BREATHING_SPACE, AWAITING_SCHEDULING);
        allow(allowed, BREATHING_SPACE, CLOSED, AWAITING_SCHEDULING);
        allowed.replaceAll((from, targets) -> Collections.unmodifiableSet(targets));
        return Collections.unmodifiableMap(allowed);
    }

    private static void allow(Map<EnforcementStatus, Set<EnforcementStatus>> allowed, EnforcementStatus from,
                              EnforcementStatus... targets) {
        allowed.get(from).addAll(Set.of(targets));
    }
}
