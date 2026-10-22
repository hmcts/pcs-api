package uk.gov.hmcts.reform.pcs.enforcement.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import uk.gov.hmcts.reform.pcs.enforcement.document.EnforcementDocumentScheduler;
import uk.gov.hmcts.reform.pcs.enforcement.domain.EnforcementDecision;
import uk.gov.hmcts.reform.pcs.enforcement.domain.EnforcementOrder;
import uk.gov.hmcts.reform.pcs.enforcement.domain.EnforcementRejectionReason;
import uk.gov.hmcts.reform.pcs.enforcement.domain.EnforcementStatus;
import uk.gov.hmcts.reform.pcs.enforcement.domain.SelectEnforcementType;
import uk.gov.hmcts.reform.pcs.enforcement.entity.EnforcementOrderEntity;
import uk.gov.hmcts.reform.pcs.enforcement.exception.NoEnforcementAwaitingReviewException;
import uk.gov.hmcts.reform.pcs.enforcement.notification.EnforcementNotificationService;
import uk.gov.hmcts.reform.pcs.enforcement.repository.EnforcementOrderRepository;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.pcs.enforcement.domain.EnforcementRejectionReason.APPLYING_OUT_OF_TIME;
import static uk.gov.hmcts.reform.pcs.enforcement.domain.EnforcementRejectionReason.ORDER_SET_ASIDE_OR_SUSPENDED;
import static uk.gov.hmcts.reform.pcs.enforcement.domain.EnforcementRejectionReason.OTHER;
import static uk.gov.hmcts.reform.pcs.enforcement.domain.EnforcementStatus.AWAITING_REVIEW;
import static uk.gov.hmcts.reform.pcs.enforcement.service.EnforcementDataUtil.buildEnforcementOrderWithSpecifiedType;

@ExtendWith(MockitoExtension.class)
class EnforcementDecisionServiceTest {

    private static final long CASE_REFERENCE = 1234L;
    private static final Instant NOW = Instant.parse("2026-10-17T09:30:00Z");

    @Mock
    private EnforcementOrderRepository enforcementOrderRepository;
    @Mock
    private CurrentEnforcementService currentEnforcementService;
    @Mock
    private EnforcementNumberGenerator enforcementNumberGenerator;
    @Mock
    private EnforcementDocumentScheduler enforcementDocumentScheduler;
    @Mock
    private EnforcementNotificationService enforcementNotificationService;
    @Mock
    private EnforcementWaTaskService enforcementWaTaskService;
    @Spy
    private EnforcementStatusTransitions enforcementStatusTransitions =
        new EnforcementStatusTransitions(Clock.fixed(NOW, ZoneOffset.UTC));
    @InjectMocks
    private EnforcementDecisionService underTest;

    @Test
    void shouldFindAnApplicationAwaitingReview() {
        givenAwaitingReview(SelectEnforcementType.WARRANT);

        assertThat(underTest.hasEnforcementAwaitingReview(CASE_REFERENCE)).isTrue();
    }

    @Test
    void shouldFindNoApplicationAwaitingReviewWhenTheClaimHasNoEnforcement() {
        givenNoEnforcement();

        assertThat(underTest.hasEnforcementAwaitingReview(CASE_REFERENCE)).isFalse();
    }

    @ParameterizedTest
    @EnumSource(value = EnforcementStatus.class, mode = EnumSource.Mode.EXCLUDE, names = "AWAITING_REVIEW")
    void shouldFindNoApplicationAwaitingReviewWhenTheCurrentEnforcementIsPastReview(EnforcementStatus status) {
        EnforcementOrderEntity enforcement = new EnforcementOrderEntity();
        enforcement.setStatus(status);
        when(currentEnforcementService.findCurrentEnforcement(CASE_REFERENCE)).thenReturn(Optional.of(enforcement));

        assertThat(underTest.hasEnforcementAwaitingReview(CASE_REFERENCE)).isFalse();
    }

    @ParameterizedTest
    @EnumSource(EnforcementStatus.class)
    void shouldOnlyLetACaseworkerDecideAnApplicationAwaitingReview(EnforcementStatus status) {
        EnforcementOrderEntity enforcement = new EnforcementOrderEntity();
        enforcement.setStatus(status);

        assertThat(EnforcementDecisionService.canDecideEnforcement(enforcement)).isEqualTo(status == AWAITING_REVIEW);
    }

    @Test
    void shouldNotLetACaseworkerDecideWhenTheClaimHasNoEnforcement() {
        assertThat(EnforcementDecisionService.canDecideEnforcement(null)).isFalse();
    }

    @ParameterizedTest
    @CsvSource({
        "WARRANT, AWAITING_BAILIFF_ALLOCATION, WP-00000001",
        "WARRANT_OF_RESTITUTION, AWAITING_BAILIFF_ALLOCATION, WR-00000002",
        "WRIT, TRANSFERRED_TO_HIGH_COURT, HP-00000003",
        "WRIT_OF_RESTITUTION, TRANSFERRED_TO_HIGH_COURT, HR-00000004",
    })
    void shouldApproveTheApplication(SelectEnforcementType type, EnforcementStatus approvedStatus, String number) {
        EnforcementOrderEntity enforcement = givenAwaitingReview(type);
        when(enforcementNumberGenerator.next(type)).thenReturn(number);

        EnforcementChange<EnforcementDecisionOutcome> decided = underTest.accept(CASE_REFERENCE);

        assertThat(decided.change())
            .isEqualTo(new EnforcementDecisionOutcome(type, EnforcementDecision.ACCEPT, number));
        assertThat(enforcement.getStatus()).isEqualTo(approvedStatus);
        assertThat(enforcement.getStatusUpdatedAt()).isEqualTo(NOW);
        assertThat(enforcement.getApprovedAt()).isEqualTo(NOW);
        assertThat(enforcement.getEnforcementNumber()).isEqualTo(number);
        assertThat(enforcement.getRejectionReason()).isNull();
        assertThat(enforcement.getRejectionReasonCode()).isNull();
        verify(enforcementStatusTransitions).transition(enforcement, approvedStatus);
        verify(enforcementOrderRepository).save(enforcement);
        verify(enforcementDocumentScheduler).scheduleApprovalDocument(enforcement.getId(), type);
        verify(enforcementNotificationService).notifyClaimantOfApproval(CASE_REFERENCE, type, number);
        if (approvedStatus == EnforcementStatus.AWAITING_BAILIFF_ALLOCATION) {
            verify(enforcementWaTaskService).warrantAwaitingBailiff(CASE_REFERENCE, enforcement.getId());
        } else {
            // An approved writ goes to the High Court, so no bailiff allocation task is created.
            verifyNoInteractions(enforcementWaTaskService);
        }
    }

    @Test
    void shouldTakeTheTypeFromTheOldEnforcementTypeFieldWhenTheNewOneIsMissing() {
        EnforcementOrderEntity enforcement = givenAwaitingReview(
            EnforcementOrder.builder().selectEnforcementType(SelectEnforcementType.WRIT).build());
        when(enforcementNumberGenerator.next(SelectEnforcementType.WRIT)).thenReturn("HP-00000009");

        underTest.accept(CASE_REFERENCE);

        assertThat(enforcement.getStatus()).isEqualTo(EnforcementStatus.TRANSFERRED_TO_HIGH_COURT);
    }

    @Test
    void shouldRefuseToApproveAnApplicationWithNoType() {
        givenAwaitingReview(EnforcementOrder.builder().build());

        assertThatThrownBy(() -> underTest.accept(CASE_REFERENCE)).isInstanceOf(IllegalStateException.class);
        verify(enforcementOrderRepository, never()).save(any());
        verifyNoInteractions(enforcementNotificationService, enforcementWaTaskService);
    }

    @Test
    void shouldApproveNothingWhenNoApplicationIsAwaitingReview() {
        givenNoEnforcement();

        assertThat(underTest.accept(CASE_REFERENCE).errors())
            .containsExactly(NoEnforcementAwaitingReviewException.MESSAGE);
        verifyNoInteractions(enforcementNumberGenerator);
        verify(enforcementOrderRepository, never()).save(any());
        verifyNoInteractions(enforcementNotificationService, enforcementWaTaskService);
    }

    @Test
    void shouldRejectTheApplicationWithTheReasonAndNoNumber() {
        EnforcementOrderEntity enforcement = givenAwaitingReview(SelectEnforcementType.WARRANT);

        EnforcementChange<EnforcementDecisionOutcome> decided = underTest.reject(
            CASE_REFERENCE, ORDER_SET_ASIDE_OR_SUSPENDED, "  The order has been set aside  ");

        assertThat(decided.change()).isEqualTo(
            new EnforcementDecisionOutcome(SelectEnforcementType.WARRANT, EnforcementDecision.REJECT, null));
        assertThat(enforcement.getStatus()).isEqualTo(EnforcementStatus.REJECTED);
        assertThat(enforcement.getStatus().isLive()).isFalse();
        assertThat(enforcement.getStatusUpdatedAt()).isEqualTo(NOW);
        assertThat(enforcement.getRejectionReasonCode()).isEqualTo(ORDER_SET_ASIDE_OR_SUSPENDED);
        assertThat(enforcement.getRejectionReason()).isEqualTo("The order has been set aside");
        assertThat(enforcement.getEnforcementNumber()).isNull();
        assertThat(enforcement.getApprovedAt()).isNull();
        verifyNoInteractions(enforcementNumberGenerator, enforcementDocumentScheduler, enforcementWaTaskService);
        verify(enforcementOrderRepository).save(enforcement);
        verify(enforcementNotificationService).notifyClaimantOfRejection(
            CASE_REFERENCE, SelectEnforcementType.WARRANT, "The order has been set aside");
    }

    @ParameterizedTest
    @EnumSource(value = EnforcementRejectionReason.class, names = {
        "APPLYING_OUT_OF_TIME",
        "APPLYING_BEFORE_POSSESSION_DATE",
        "NOT_TRANSFERRED_TO_HIGH_COURT",
        "ANOTHER_ENFORCEMENT_JOURNEY_ONGOING",
        "DEFENDANTS_IN_BREATHING_SPACE"})
    void shouldRejectTheApplicationOnAReasonThatStandsAlone(EnforcementRejectionReason reason) {
        EnforcementOrderEntity enforcement = givenAwaitingReview(SelectEnforcementType.WARRANT);

        EnforcementChange<EnforcementDecisionOutcome> decided = underTest.reject(CASE_REFERENCE, reason, null);

        assertThat(decided.refused()).isFalse();
        assertThat(enforcement.getStatus()).isEqualTo(EnforcementStatus.REJECTED);
        assertThat(enforcement.getRejectionReasonCode()).isEqualTo(reason);
        assertThat(enforcement.getRejectionReason()).isNull();
        verify(enforcementOrderRepository).save(enforcement);
        verify(enforcementNotificationService).notifyClaimantOfRejection(
            CASE_REFERENCE, SelectEnforcementType.WARRANT, null);
    }

    @Test
    void shouldDiscardDetailTypedUnderAReasonThatWasThenChangedForOneStandingAlone() {
        EnforcementOrderEntity enforcement = givenAwaitingReview(SelectEnforcementType.WARRANT);

        underTest.reject(CASE_REFERENCE, APPLYING_OUT_OF_TIME, "Typed under \"Other\" before changing my mind");

        assertThat(enforcement.getRejectionReasonCode()).isEqualTo(APPLYING_OUT_OF_TIME);
        assertThat(enforcement.getRejectionReason()).isNull();
        verify(enforcementNotificationService).notifyClaimantOfRejection(
            CASE_REFERENCE, SelectEnforcementType.WARRANT, null);
    }

    @Test
    void shouldRefuseToRejectWithoutASelectedReason() {
        assertThat(underTest.reject(CASE_REFERENCE, null, "The order has been set aside").errors())
            .containsExactly(EnforcementDecisionService.NO_REJECTION_REASON_SELECTED_MESSAGE);
        verify(enforcementOrderRepository, never()).save(any());
        verifyNoInteractions(enforcementNotificationService, enforcementWaTaskService);
    }

    @ParameterizedTest
    @MethodSource("reasonsNeedingDetailWithoutIt")
    void shouldRefuseToRejectWithoutTheDetailAReasonNeeds(EnforcementRejectionReason reason, String detail) {
        assertThat(underTest.reject(CASE_REFERENCE, reason, detail).errors())
            .containsExactly(EnforcementDecisionService.NO_REJECTION_REASON_MESSAGE);
        verify(enforcementOrderRepository, never()).save(any());
        verifyNoInteractions(enforcementNotificationService, enforcementWaTaskService);
    }

    private static Stream<Arguments> reasonsNeedingDetailWithoutIt() {
        return EnforcementRejectionReason.needingDetail().stream()
            .flatMap(reason -> Stream.of(null, "", "   ").map(detail -> Arguments.of(reason, detail)));
    }

    @Test
    void shouldRejectNothingWhenNoApplicationIsAwaitingReview() {
        givenNoEnforcement();

        assertThat(underTest.reject(CASE_REFERENCE, OTHER, "reason").errors())
            .containsExactly(NoEnforcementAwaitingReviewException.MESSAGE);
        verify(enforcementOrderRepository, never()).save(any());
        verifyNoInteractions(enforcementNotificationService, enforcementWaTaskService);
    }

    @Test
    void shouldRefuseTheApprovalWhenTheWarrantCannotBeQueued() {
        givenAwaitingReview(SelectEnforcementType.WARRANT);
        when(enforcementNumberGenerator.next(SelectEnforcementType.WARRANT)).thenReturn("WP-00000001");
        doThrow(new DataIntegrityViolationException("scheduled_tasks"))
            .when(enforcementDocumentScheduler).scheduleApprovalDocument(any(), any());

        assertThat(underTest.accept(CASE_REFERENCE).errors())
            .containsExactly(EnforcementSideEffects.COULD_NOT_SCHEDULE_ERROR);
    }

    @Test
    void shouldRefuseTheApprovalWhenTheAllocationTaskCannotBeRequested() {
        givenAwaitingReview(SelectEnforcementType.WARRANT);
        when(enforcementNumberGenerator.next(SelectEnforcementType.WARRANT)).thenReturn("WP-00000001");
        doThrow(new DataIntegrityViolationException("scheduled_tasks"))
            .when(enforcementWaTaskService).warrantAwaitingBailiff(anyLong(), any());

        assertThat(underTest.accept(CASE_REFERENCE).errors())
            .containsExactly(EnforcementSideEffects.COULD_NOT_SCHEDULE_ERROR);
    }

    @Test
    void shouldRefuseTheRejectionWhenTheClaimantsEmailCannotBeQueued() {
        givenAwaitingReview(SelectEnforcementType.WARRANT);
        doThrow(new DataIntegrityViolationException("case_notification"))
            .when(enforcementNotificationService).notifyClaimantOfRejection(anyLong(), any(), anyString());

        assertThat(underTest.reject(CASE_REFERENCE, ORDER_SET_ASIDE_OR_SUSPENDED, "The order has been set aside")
                       .errors())
            .containsExactly(EnforcementSideEffects.COULD_NOT_SCHEDULE_ERROR);
    }

    private EnforcementOrderEntity givenAwaitingReview(SelectEnforcementType type) {
        return givenAwaitingReview(buildEnforcementOrderWithSpecifiedType(type));
    }

    private EnforcementOrderEntity givenAwaitingReview(EnforcementOrder enforcementOrder) {
        EnforcementOrderEntity enforcement = new EnforcementOrderEntity();
        enforcement.setId(UUID.randomUUID());
        enforcement.setEnforcementOrder(enforcementOrder);
        enforcement.setStatus(AWAITING_REVIEW);
        when(currentEnforcementService.findCurrentEnforcement(CASE_REFERENCE)).thenReturn(Optional.of(enforcement));
        return enforcement;
    }

    private void givenNoEnforcement() {
        when(currentEnforcementService.findCurrentEnforcement(CASE_REFERENCE)).thenReturn(Optional.empty());
    }
}
