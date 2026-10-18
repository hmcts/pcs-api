package uk.gov.hmcts.reform.pcs.enforcement.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.pcs.enforcement.domain.EnforcementStatus;
import uk.gov.hmcts.reform.pcs.enforcement.entity.EnforcementOrderEntity;
import uk.gov.hmcts.reform.pcs.enforcement.exception.EnforcementEventNotAvailableException;
import uk.gov.hmcts.reform.pcs.enforcement.exception.NoEnforcementException;
import uk.gov.hmcts.reform.pcs.enforcement.notification.EnforcementNotificationService;
import uk.gov.hmcts.reform.pcs.enforcement.repository.EnforcementOrderRepository;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.pcs.enforcement.domain.EnforcementStatus.AWAITING_CLAIMANT_CONFIRMATION;
import static uk.gov.hmcts.reform.pcs.enforcement.domain.EnforcementStatus.AWAITING_SCHEDULING;
import static uk.gov.hmcts.reform.pcs.enforcement.domain.EnforcementStatus.CLAIMANT_CONFIRMED;
import static uk.gov.hmcts.reform.pcs.enforcement.domain.EnforcementStatus.CLAIMANT_UNABLE_TO_ATTEND;
import static uk.gov.hmcts.reform.pcs.enforcement.domain.EnforcementStatus.CLOSED;

@ExtendWith(MockitoExtension.class)
class EvictionScheduleServiceTest {

    private static final long CASE_REFERENCE = 1234L;
    private static final Instant NOW = Instant.parse("2026-06-20T09:00:00Z");
    private static final LocalDateTime EVICTION = LocalDateTime.parse("2026-06-25T10:00");
    private static final LocalDateTime SERVED = LocalDateTime.parse("2026-06-21T08:00");

    @Mock
    private CurrentEnforcementService currentEnforcementService;
    @Mock
    private EnforcementOrderRepository enforcementOrderRepository;
    @Mock
    private BailiffNoteService bailiffNoteService;
    @Mock
    private EnforcementNotificationService enforcementNotificationService;

    private EvictionScheduleService underTest;

    @BeforeEach
    void setUp() {
        underTest = new EvictionScheduleService(currentEnforcementService, enforcementOrderRepository,
                                                new EnforcementStatusTransitions(Clock.fixed(NOW, ZoneOffset.UTC)),
                                                bailiffNoteService, enforcementNotificationService);
    }

    @Test
    void shouldScheduleTheEvictionAsTheBailiffDateAndAwaitClaimantConfirmation() {
        EnforcementOrderEntity enforcement = current(AWAITING_SCHEDULING);
        enforcement.setEnforcementNumber("WP-00000042");

        EnforcementChange<String> scheduled = underTest.schedule(CASE_REFERENCE, EVICTION, false);

        assertThat(scheduled.refused()).isFalse();
        assertThat(scheduled.change()).isEqualTo("WP-00000042");
        assertThat(enforcement.getBailiffDate()).isEqualTo(EVICTION);
        assertThat(enforcement.getDispenseWithService()).isFalse();
        assertThat(enforcement.getStatus()).isEqualTo(AWAITING_CLAIMANT_CONFIRMATION);
        assertThat(enforcement.getStatusUpdatedAt()).isEqualTo(NOW);
        assertThat(enforcement.isDefendantsCanSeeEnforcement()).isFalse();
        verify(enforcementOrderRepository).save(enforcement);
        verify(enforcementNotificationService).notifyClaimantOfScheduledEviction(CASE_REFERENCE, EVICTION);
    }

    @Test
    void shouldRecordThatServiceIsDispensedWith() {
        EnforcementOrderEntity enforcement = current(AWAITING_SCHEDULING);

        underTest.schedule(CASE_REFERENCE, EVICTION, true);

        assertThat(enforcement.getDispenseWithService()).isTrue();
        assertThat(enforcement.isDefendantsCanSeeEnforcement()).isFalse();
    }

    @ParameterizedTest
    @EnumSource(value = EnforcementStatus.class, names = "AWAITING_SCHEDULING", mode = EnumSource.Mode.EXCLUDE)
    void shouldOnlyScheduleAWarrantAwaitingScheduling(EnforcementStatus status) {
        EnforcementOrderEntity enforcement = current(status);

        EnforcementChange<String> scheduled = underTest.schedule(CASE_REFERENCE, EVICTION, false);

        assertThat(scheduled.refused()).isTrue();
        assertThat(scheduled.errors()).containsExactly(status.isLive()
                                                           ? EvictionScheduleService.CANNOT_SCHEDULE_ERROR
                                                           : NoEnforcementException.MESSAGE);
        assertThat(enforcement.getBailiffDate()).isNull();
        verify(enforcementOrderRepository, never()).save(any());
        verifyNoInteractions(enforcementNotificationService);
    }

    @Test
    void shouldRefuseToScheduleRatherThanThrowWhenTheClaimHasNoEnforcement() {
        when(currentEnforcementService.findCurrentEnforcement(CASE_REFERENCE)).thenReturn(Optional.empty());

        EnforcementChange<String> scheduled = underTest.schedule(CASE_REFERENCE, EVICTION, false);

        assertThat(scheduled.errors()).containsExactly(NoEnforcementException.MESSAGE);
    }

    @Test
    void shouldRefuseToScheduleWithoutALiveWarrant() {
        current(CLOSED);

        assertThatThrownBy(() -> underTest.checkCanSchedule(CASE_REFERENCE))
            .isInstanceOf(NoEnforcementException.class)
            .hasMessage("There is no warrant on this case");
    }

    @Test
    void shouldRefuseToScheduleWhenTheClaimHasNoEnforcement() {
        when(currentEnforcementService.findCurrentEnforcement(CASE_REFERENCE)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> underTest.checkCanSchedule(CASE_REFERENCE))
            .isInstanceOf(NoEnforcementException.class);
    }

    @Test
    void shouldRefuseToScheduleAnEvictionThatIsAlreadyScheduled() {
        current(AWAITING_CLAIMANT_CONFIRMATION);

        assertThatThrownBy(() -> underTest.checkCanSchedule(CASE_REFERENCE))
            .isInstanceOf(EnforcementEventNotAvailableException.class)
            .hasMessage(EvictionScheduleService.CANNOT_SCHEDULE_ERROR);
    }

    @ParameterizedTest
    @EnumSource(value = EnforcementStatus.class, names = {"CLAIMANT_UNABLE_TO_ATTEND", "CLAIMANT_CONFIRMED"})
    void shouldRescheduleOnceTheClaimantHasAnswered(EnforcementStatus status) {
        EnforcementOrderEntity enforcement = current(status);
        enforcement.setBailiffDate(EVICTION);
        enforcement.setEnforcementNumber("WP-00000042");
        LocalDateTime newDate = LocalDateTime.parse("2026-07-01T09:30");

        EnforcementChange<String> rescheduled = underTest.reschedule(CASE_REFERENCE, newDate, false);

        assertThat(rescheduled.change()).isEqualTo("WP-00000042");
        assertThat(enforcement.getBailiffDate()).isEqualTo(newDate);
        assertThat(enforcement.getStatus()).isEqualTo(AWAITING_CLAIMANT_CONFIRMATION);
        verify(enforcementOrderRepository).save(enforcement);
        verify(enforcementNotificationService).notifyClaimantOfRescheduledEviction(CASE_REFERENCE, newDate);
    }

    @ParameterizedTest
    @EnumSource(value = EnforcementStatus.class, names = {"CLAIMANT_UNABLE_TO_ATTEND", "CLAIMANT_CONFIRMED"},
        mode = EnumSource.Mode.EXCLUDE)
    void shouldNotRescheduleInAnyOtherStatus(EnforcementStatus status) {
        current(status);

        assertThatThrownBy(() -> underTest.checkCanReschedule(CASE_REFERENCE))
            .isInstanceOfAny(EnforcementEventNotAvailableException.class, NoEnforcementException.class);
    }

    @ParameterizedTest
    @EnumSource(value = EnforcementStatus.class, names = {"CLAIMANT_UNABLE_TO_ATTEND", "CLAIMANT_CONFIRMED"},
        mode = EnumSource.Mode.EXCLUDE)
    void shouldNotEmailTheClaimantWhenTheRescheduleIsRefused(EnforcementStatus status) {
        current(status);

        EnforcementChange<String> rescheduled = underTest.reschedule(CASE_REFERENCE, EVICTION, false);

        assertThat(rescheduled.refused()).isTrue();
        verifyNoInteractions(enforcementNotificationService);
    }

    @Test
    void shouldClearThePreviousNoticeServiceWhenRescheduling() {
        EnforcementOrderEntity enforcement = current(CLAIMANT_CONFIRMED);
        enforcement.setBailiffDate(EVICTION);
        enforcement.setDispenseWithService(false);
        enforcement.setNoticeServedAt(SERVED);
        enforcement.setNoticeServedHow("by hand to the occupant");
        enforcement.setClaimantCanAttend(true);
        enforcement.setDefendantsCanSeeEnforcement(true);

        underTest.reschedule(CASE_REFERENCE, EVICTION.plusDays(7), false);

        assertThat(enforcement.getNoticeServedAt()).isNull();
        assertThat(enforcement.getClaimantCanAttend()).isNull();
        assertThat(enforcement.getNoticeServedHow()).isNull();
        assertThat(enforcement.isDefendantsCanSeeEnforcement()).isFalse();
        assertThat(EvictionScheduleService.canMarkNoticeServed(enforcement)).isTrue();
    }

    @Test
    void shouldHideTheEnforcementFromDefendantsWhenRescheduledWithoutNotice() {
        EnforcementOrderEntity enforcement = current(CLAIMANT_UNABLE_TO_ATTEND);
        enforcement.setDispenseWithService(false);
        enforcement.setNoticeServedAt(SERVED);
        enforcement.setDefendantsCanSeeEnforcement(true);

        underTest.reschedule(CASE_REFERENCE, EVICTION, true);

        assertThat(enforcement.isDefendantsCanSeeEnforcement()).isFalse();
        assertThat(EvictionScheduleService.canMarkNoticeServed(enforcement)).isFalse();
    }

    @Test
    void shouldMarkTheNoticeServedAndLetDefendantsSeeTheEnforcement() {
        EnforcementOrderEntity enforcement = scheduled(AWAITING_CLAIMANT_CONFIRMATION, false);
        enforcement.setEnforcementNumber("WP-00000042");

        EnforcementChange<String> served =
            underTest.markNoticeServed(CASE_REFERENCE, SERVED, "by hand to the occupant", null);

        assertThat(served.change()).isEqualTo("WP-00000042");
        assertThat(enforcement.getNoticeServedAt()).isEqualTo(SERVED);
        assertThat(enforcement.getNoticeServedHow()).isEqualTo("by hand to the occupant");
        assertThat(enforcement.isDefendantsCanSeeEnforcement()).isTrue();
        assertThat(enforcement.getStatus()).isEqualTo(AWAITING_CLAIMANT_CONFIRMATION);
        verify(enforcementOrderRepository).save(enforcement);
        verify(bailiffNoteService, never()).addNote(anyLong(), anyString());
    }

    @Test
    void shouldAddTheBailiffNoteWhenMarkingTheNoticeServed() {
        scheduled(CLAIMANT_CONFIRMED, false);

        underTest.markNoticeServed(CASE_REFERENCE, SERVED, "by hand", "Spoke to occupant at the door");

        verify(bailiffNoteService).addNote(CASE_REFERENCE, "Spoke to occupant at the door");
    }

    @Test
    void shouldOfferToMarkTheNoticeServedOnlyWhenItIsDue() {
        assertThat(EvictionScheduleService.canMarkNoticeServed(scheduledEntity(AWAITING_CLAIMANT_CONFIRMATION, false)))
            .isTrue();
        assertThat(EvictionScheduleService.canMarkNoticeServed(scheduledEntity(CLAIMANT_UNABLE_TO_ATTEND, false)))
            .isTrue();
        assertThat(EvictionScheduleService.canMarkNoticeServed(scheduledEntity(CLAIMANT_CONFIRMED, false)))
            .isTrue();
        // dispensed with service
        assertThat(EvictionScheduleService.canMarkNoticeServed(scheduledEntity(AWAITING_CLAIMANT_CONFIRMATION, true)))
            .isFalse();
        // not scheduled
        assertThat(EvictionScheduleService.canMarkNoticeServed(entity(AWAITING_SCHEDULING))).isFalse();
        // already served
        EnforcementOrderEntity served = scheduledEntity(AWAITING_CLAIMANT_CONFIRMATION, false);
        served.setNoticeServedAt(SERVED);
        assertThat(EvictionScheduleService.canMarkNoticeServed(served)).isFalse();
        // after the eviction
        assertThat(EvictionScheduleService.canMarkNoticeServed(scheduledEntity(CLOSED, false))).isFalse();
    }

    @Test
    void shouldRefuseToMarkTheNoticeServedTwice() {
        EnforcementOrderEntity enforcement = scheduled(AWAITING_CLAIMANT_CONFIRMATION, false);
        enforcement.setNoticeServedAt(SERVED);

        EnforcementChange<String> served = underTest.markNoticeServed(CASE_REFERENCE, SERVED, "by hand", null);

        assertThat(served.errors()).containsExactly(EvictionScheduleService.CANNOT_MARK_NOTICE_SERVED_ERROR);
        verify(enforcementOrderRepository, never()).save(any());
    }

    @Test
    void shouldRefuseToMarkTheNoticeServedWhenServiceIsDispensedWith() {
        scheduled(AWAITING_CLAIMANT_CONFIRMATION, true);

        assertThatThrownBy(() -> underTest.checkCanMarkNoticeServed(CASE_REFERENCE))
            .isInstanceOf(EnforcementEventNotAvailableException.class);
    }

    @Test
    void shouldAllowTheChecksWhenTheEventApplies() {
        current(AWAITING_SCHEDULING);
        assertThatNoException().isThrownBy(() -> underTest.checkCanSchedule(CASE_REFERENCE));
    }

    @Test
    void shouldDeriveDefendantVisibilityFromTheNotice() {
        EnforcementOrderEntity enforcement = entity(AWAITING_CLAIMANT_CONFIRMATION);
        assertThat(EvictionScheduleService.defendantsCanSeeEnforcement(enforcement)).isFalse();

        enforcement.setDispenseWithService(false);
        assertThat(EvictionScheduleService.defendantsCanSeeEnforcement(enforcement)).isFalse();

        enforcement.setNoticeServedAt(SERVED);
        assertThat(EvictionScheduleService.defendantsCanSeeEnforcement(enforcement)).isTrue();

        enforcement.setDispenseWithService(true);
        assertThat(EvictionScheduleService.defendantsCanSeeEnforcement(enforcement)).isFalse();
    }

    private EnforcementOrderEntity current(EnforcementStatus status) {
        EnforcementOrderEntity enforcement = entity(status);
        when(currentEnforcementService.findCurrentEnforcement(CASE_REFERENCE)).thenReturn(Optional.of(enforcement));
        return enforcement;
    }

    private EnforcementOrderEntity scheduled(EnforcementStatus status, boolean dispenseWithService) {
        EnforcementOrderEntity enforcement = scheduledEntity(status, dispenseWithService);
        when(currentEnforcementService.findCurrentEnforcement(CASE_REFERENCE)).thenReturn(Optional.of(enforcement));
        return enforcement;
    }

    private static EnforcementOrderEntity scheduledEntity(EnforcementStatus status, boolean dispenseWithService) {
        EnforcementOrderEntity enforcement = entity(status);
        enforcement.setBailiffDate(EVICTION);
        enforcement.setDispenseWithService(dispenseWithService);
        return enforcement;
    }

    private static EnforcementOrderEntity entity(EnforcementStatus status) {
        EnforcementOrderEntity enforcement = new EnforcementOrderEntity();
        enforcement.setId(UUID.randomUUID());
        enforcement.setStatus(status);
        return enforcement;
    }
}
