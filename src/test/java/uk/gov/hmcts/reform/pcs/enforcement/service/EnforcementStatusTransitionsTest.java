package uk.gov.hmcts.reform.pcs.enforcement.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import uk.gov.hmcts.reform.pcs.enforcement.domain.EnforcementStatus;
import uk.gov.hmcts.reform.pcs.enforcement.entity.EnforcementOrderEntity;
import uk.gov.hmcts.reform.pcs.enforcement.exception.IllegalEnforcementStatusTransitionException;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static uk.gov.hmcts.reform.pcs.enforcement.domain.EnforcementStatus.AWAITING_BAILIFF_ALLOCATION;
import static uk.gov.hmcts.reform.pcs.enforcement.domain.EnforcementStatus.AWAITING_CLAIMANT_CONFIRMATION;
import static uk.gov.hmcts.reform.pcs.enforcement.domain.EnforcementStatus.AWAITING_PAYMENT;
import static uk.gov.hmcts.reform.pcs.enforcement.domain.EnforcementStatus.AWAITING_REVIEW;
import static uk.gov.hmcts.reform.pcs.enforcement.domain.EnforcementStatus.AWAITING_SCHEDULING;
import static uk.gov.hmcts.reform.pcs.enforcement.domain.EnforcementStatus.BREATHING_SPACE;
import static uk.gov.hmcts.reform.pcs.enforcement.domain.EnforcementStatus.CLAIMANT_CONFIRMED;
import static uk.gov.hmcts.reform.pcs.enforcement.domain.EnforcementStatus.CLAIMANT_UNABLE_TO_ATTEND;
import static uk.gov.hmcts.reform.pcs.enforcement.domain.EnforcementStatus.CLOSED;
import static uk.gov.hmcts.reform.pcs.enforcement.domain.EnforcementStatus.REJECTED;
import static uk.gov.hmcts.reform.pcs.enforcement.domain.EnforcementStatus.TRANSFERRED_TO_HIGH_COURT;

class EnforcementStatusTransitionsTest {

    private static final Instant NOW = Instant.parse("2026-10-16T10:15:30Z");

    /**
     * The transition table from 19-enforcement-status-model.feature, plus the two additions noted below.
     * A null "from" is the feature's "(none)".
     */
    private static final List<Transition> TABLE = List.of(
        new Transition(null, "the claimant submits an application", AWAITING_PAYMENT),
        // 01-claimant-applies-for-enforcement.feature: a repeat warrant with no fee goes straight to review
        new Transition(null, "the claimant submits an application with no fee due", AWAITING_REVIEW),
        new Transition(AWAITING_PAYMENT, "the claimant pays", AWAITING_REVIEW),
        new Transition(AWAITING_REVIEW, "the caseworker approves a warrant", AWAITING_BAILIFF_ALLOCATION),
        new Transition(AWAITING_REVIEW, "the caseworker rejects the application", REJECTED),
        // README open question 10: an approved writ goes to a High Court Enforcement Officer
        new Transition(AWAITING_REVIEW, "the caseworker approves a writ", TRANSFERRED_TO_HIGH_COURT),
        new Transition(AWAITING_BAILIFF_ALLOCATION, "a bailiff is allocated", AWAITING_SCHEDULING),
        new Transition(AWAITING_SCHEDULING, "the bailiff schedules the eviction", AWAITING_CLAIMANT_CONFIRMATION),
        new Transition(AWAITING_CLAIMANT_CONFIRMATION, "the claimant confirms they can attend", CLAIMANT_CONFIRMED),
        new Transition(AWAITING_CLAIMANT_CONFIRMATION, "the claimant says they cannot attend",
                       CLAIMANT_UNABLE_TO_ATTEND),
        new Transition(CLAIMANT_UNABLE_TO_ATTEND, "the bailiff reschedules the eviction",
                       AWAITING_CLAIMANT_CONFIRMATION),
        new Transition(CLAIMANT_CONFIRMED, "the bailiff records a closing final return", CLOSED),
        new Transition(CLAIMANT_CONFIRMED, "the bailiff records a breathing space return", BREATHING_SPACE),
        new Transition(CLAIMANT_CONFIRMED, "the bailiff records an obstruction return", AWAITING_SCHEDULING),
        new Transition(CLOSED, "the bailiff records \"Reopen warrant\"", AWAITING_BAILIFF_ALLOCATION)
    );

    private final EnforcementStatusTransitions underTest =
        new EnforcementStatusTransitions(Clock.fixed(NOW, ZoneOffset.UTC));

    @ParameterizedTest(name = "{0} -> {1}: {2}")
    @MethodSource("everyPairOfStatuses")
    void shouldAllowExactlyTheTransitionsInTheTable(EnforcementStatus from, EnforcementStatus to, boolean allowed) {
        assertThat(EnforcementStatusTransitions.isAllowed(from, to)).isEqualTo(allowed);
    }

    @ParameterizedTest(name = "{0} -> {2} when {1}")
    @MethodSource("tableRows")
    void shouldMoveTheEnforcementAndRecordWhen(EnforcementStatus from, String trigger, EnforcementStatus to) {
        EnforcementOrderEntity enforcement = enforcementWithStatus(from);

        underTest.transition(enforcement, to);

        assertThat(enforcement.getStatus()).isEqualTo(to);
        assertThat(enforcement.getStatusUpdatedAt()).isEqualTo(NOW);
    }

    @ParameterizedTest(name = "{0} -> {1}")
    @MethodSource("illegalTransitions")
    void shouldRejectATransitionNotInTheTable(EnforcementStatus from, EnforcementStatus to) {
        EnforcementOrderEntity enforcement = enforcementWithStatus(from);
        Instant previousUpdate = enforcement.getStatusUpdatedAt();

        assertThatThrownBy(() -> underTest.transition(enforcement, to))
            .isInstanceOf(IllegalEnforcementStatusTransitionException.class)
            .hasMessage("Enforcement %s cannot move from %s to %s",
                        enforcement.getId(), from == null ? "no status" : from, to);
        assertThat(enforcement.getStatus()).isEqualTo(from);
        assertThat(enforcement.getStatusUpdatedAt()).isEqualTo(previousUpdate);
    }

    @Test
    void shouldRejectANullTarget() {
        EnforcementOrderEntity enforcement = enforcementWithStatus(AWAITING_PAYMENT);

        assertThatThrownBy(() -> underTest.transition(enforcement, null))
            .isInstanceOf(IllegalEnforcementStatusTransitionException.class);
    }

    @Test
    void shouldHaveNoWayOutOfTheTerminalStatusesOtherThanReopeningAClosedWarrant() {
        assertThat(EnforcementStatusTransitions.allowedTargets(REJECTED)).isEmpty();
        assertThat(EnforcementStatusTransitions.allowedTargets(TRANSFERRED_TO_HIGH_COURT)).isEmpty();
        assertThat(EnforcementStatusTransitions.allowedTargets(CLOSED)).containsExactly(AWAITING_BAILIFF_ALLOCATION);
    }

    @Test
    void shouldStartInAwaitingPaymentUnlessNoPaymentIsOutstanding() {
        assertThat(EnforcementStatusTransitions.initialStatus(true)).isEqualTo(AWAITING_PAYMENT);
        assertThat(EnforcementStatusTransitions.initialStatus(false)).isEqualTo(AWAITING_REVIEW);
    }

    private static Stream<Arguments> tableRows() {
        return TABLE.stream().map(row -> arguments(row.from(), row.trigger(), row.to()));
    }

    private static Stream<Arguments> everyPairOfStatuses() {
        return fromStatuses().stream().flatMap(from -> Arrays.stream(EnforcementStatus.values())
            .map(to -> arguments(from, to, inTable(from, to))));
    }

    private static Stream<Arguments> illegalTransitions() {
        return everyPairOfStatuses().filter(args -> !((Boolean) args.get()[2]))
            .map(args -> arguments(args.get()[0], args.get()[1]));
    }

    private static List<EnforcementStatus> fromStatuses() {
        List<EnforcementStatus> from = new ArrayList<>();
        from.add(null);
        from.addAll(Arrays.asList(EnforcementStatus.values()));
        return from;
    }

    private static boolean inTable(EnforcementStatus from, EnforcementStatus to) {
        return TABLE.stream().anyMatch(row -> row.from() == from && row.to() == to);
    }

    private static EnforcementOrderEntity enforcementWithStatus(EnforcementStatus status) {
        EnforcementOrderEntity enforcement = new EnforcementOrderEntity();
        enforcement.setId(UUID.randomUUID());
        enforcement.setStatus(status);
        enforcement.setStatusUpdatedAt(status == null ? null : NOW.minusSeconds(60));
        return enforcement;
    }

    private record Transition(EnforcementStatus from, String trigger, EnforcementStatus to) {
    }
}
