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

@Component
@RequiredArgsConstructor
public class EnforcementStatusTransitions {

    static final Set<EnforcementStatus> INITIAL = Collections.unmodifiableSet(
        EnumSet.of(AWAITING_PAYMENT, AWAITING_REVIEW));

    private static final Map<EnforcementStatus, Set<EnforcementStatus>> ALLOWED = allowedTransitions();

    private final Clock utcClock;

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

    public static EnforcementStatus initialStatus(boolean paymentOutstanding) {
        return paymentOutstanding ? AWAITING_PAYMENT : AWAITING_REVIEW;
    }

    private static Map<EnforcementStatus, Set<EnforcementStatus>> allowedTransitions() {
        Map<EnforcementStatus, Set<EnforcementStatus>> allowed = new EnumMap<>(EnforcementStatus.class);
        for (EnforcementStatus status : EnforcementStatus.values()) {
            allowed.put(status, EnumSet.noneOf(EnforcementStatus.class));
        }
        allow(allowed, AWAITING_PAYMENT, AWAITING_REVIEW);
        allow(allowed, AWAITING_REVIEW, AWAITING_BAILIFF_ALLOCATION, REJECTED, TRANSFERRED_TO_HIGH_COURT);
        allow(allowed, AWAITING_BAILIFF_ALLOCATION, AWAITING_SCHEDULING);
        allow(allowed, AWAITING_SCHEDULING, AWAITING_CLAIMANT_CONFIRMATION);
        allow(allowed, AWAITING_CLAIMANT_CONFIRMATION, CLAIMANT_CONFIRMED, CLAIMANT_UNABLE_TO_ATTEND);
        allow(allowed, CLAIMANT_UNABLE_TO_ATTEND, AWAITING_CLAIMANT_CONFIRMATION);
        allow(allowed, CLAIMANT_CONFIRMED, CLOSED, BREATHING_SPACE, AWAITING_SCHEDULING);
        allow(allowed, CLAIMANT_CONFIRMED, AWAITING_CLAIMANT_CONFIRMATION);
        allow(allowed, CLOSED, AWAITING_BAILIFF_ALLOCATION);
        // A warrant return can be recorded any time after a bailiff is allocated, adding to the targets above.
        allow(allowed, AWAITING_BAILIFF_ALLOCATION, CLOSED, BREATHING_SPACE);
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
