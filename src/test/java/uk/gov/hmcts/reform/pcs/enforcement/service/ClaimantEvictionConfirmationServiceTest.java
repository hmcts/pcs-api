package uk.gov.hmcts.reform.pcs.enforcement.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.service.PcsCaseService;
import uk.gov.hmcts.reform.pcs.enforcement.domain.EnforcementOrder;
import uk.gov.hmcts.reform.pcs.enforcement.domain.EnforcementStatus;
import uk.gov.hmcts.reform.pcs.enforcement.entity.EnforcementOrderEntity;
import uk.gov.hmcts.reform.pcs.enforcement.notification.EnforcementNotificationService;
import uk.gov.hmcts.reform.pcs.enforcement.repository.EnforcementOrderRepository;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.pcs.enforcement.domain.EnforcementStatus.AWAITING_CLAIMANT_CONFIRMATION;
import static uk.gov.hmcts.reform.pcs.enforcement.domain.EnforcementStatus.CLAIMANT_CONFIRMED;
import static uk.gov.hmcts.reform.pcs.enforcement.domain.EnforcementStatus.CLAIMANT_UNABLE_TO_ATTEND;
import static uk.gov.hmcts.reform.pcs.enforcement.domain.EnforcementStatus.CLOSED;
import static uk.gov.hmcts.reform.pcs.enforcement.service.EnforcementDataUtil.buildPcsCaseEntity;

@ExtendWith(MockitoExtension.class)
class ClaimantEvictionConfirmationServiceTest {

    private static final long CASE_REFERENCE = 1234L;
    private static final UUID CLAIM_ID = UUID.randomUUID();
    private static final Instant NOW = Instant.parse("2026-10-17T09:00:00Z");
    private static final Instant EARLIER = Instant.parse("2026-10-01T09:00:00Z");
    private static final LocalDateTime EVICTION = LocalDateTime.parse("2026-10-25T10:00");

    @Mock
    private PcsCaseService pcsCaseService;
    @Mock
    private EnforcementOrderRepository enforcementOrderRepository;
    @Mock
    private ClaimantRiskAssessmentService claimantRiskAssessmentService;
    @Mock
    private EnforcementNotificationService enforcementNotificationService;
    @Mock
    private EnforcementWaTaskService enforcementWaTaskService;

    private ClaimantEvictionConfirmationService underTest;

    @BeforeEach
    void setUp() {
        underTest = new ClaimantEvictionConfirmationService(
            new CurrentEnforcementService(pcsCaseService, enforcementOrderRepository), enforcementOrderRepository,
            new EnforcementStatusTransitions(Clock.fixed(NOW, ZoneOffset.UTC)), claimantRiskAssessmentService,
            enforcementNotificationService, enforcementWaTaskService);
    }

    @Test
    void shouldMoveToClaimantConfirmedWhenTheClaimantCanAttend() {
        EnforcementOrderEntity enforcement = givenEnforcements(enforcementWithStatus(AWAITING_CLAIMANT_CONFIRMATION))
            .getFirst();

        assertThat(underTest.recordAttendance(CASE_REFERENCE, YesOrNo.YES).refused()).isFalse();

        assertThat(enforcement.getStatus()).isEqualTo(CLAIMANT_CONFIRMED);
        assertThat(enforcement.getClaimantCanAttend()).isTrue();
        assertThat(enforcement.getStatusUpdatedAt()).isEqualTo(NOW);
        verify(enforcementOrderRepository).save(enforcement);
        verify(enforcementNotificationService).notifyClaimantOfConfirmedAttendance(CASE_REFERENCE, EVICTION);
        verify(enforcementNotificationService, never()).notifyClaimantOfDeclinedAttendance(CASE_REFERENCE);
        verifyNoInteractions(enforcementWaTaskService);
    }

    @Test
    void shouldMoveToClaimantUnableToAttendWhenTheClaimantCannotAttend() {
        EnforcementOrderEntity enforcement = givenEnforcements(enforcementWithStatus(AWAITING_CLAIMANT_CONFIRMATION))
            .getFirst();

        underTest.recordAttendance(CASE_REFERENCE, YesOrNo.NO);

        assertThat(enforcement.getStatus()).isEqualTo(CLAIMANT_UNABLE_TO_ATTEND);
        assertThat(enforcement.getClaimantCanAttend()).isFalse();
        assertThat(enforcement.getStatusUpdatedAt()).isEqualTo(NOW);
        verify(enforcementOrderRepository).save(enforcement);
        verify(enforcementNotificationService).notifyClaimantOfDeclinedAttendance(CASE_REFERENCE);
        verify(enforcementNotificationService, never()).notifyClaimantOfConfirmedAttendance(anyLong(), any());
        verify(enforcementWaTaskService).claimantCannotAttend(CASE_REFERENCE, enforcement.getId(), EVICTION);
    }

    /**
     * The claimant is asked to try again rather than given an unexplained 500, and their answer is rolled
     * back with the email.
     */
    @Test
    void shouldRefuseTheAnswerWhenTheClaimantsEmailCannotBeQueued() {
        givenEnforcements(enforcementWithStatus(AWAITING_CLAIMANT_CONFIRMATION));
        doThrow(new DataIntegrityViolationException("case_notification"))
            .when(enforcementNotificationService).notifyClaimantOfConfirmedAttendance(anyLong(), any());

        EnforcementChange<Void> recorded = underTest.recordAttendance(CASE_REFERENCE, YesOrNo.YES);

        assertThat(recorded.errors()).containsExactly(EnforcementSideEffects.COULD_NOT_SCHEDULE_ERROR);
    }

    @Test
    void shouldRefuseTheAnswerWhenTheRescheduleTaskCannotBeRequested() {
        givenEnforcements(enforcementWithStatus(AWAITING_CLAIMANT_CONFIRMATION));
        doThrow(new DataIntegrityViolationException("scheduled_tasks"))
            .when(enforcementWaTaskService).claimantCannotAttend(anyLong(), any(), any());

        EnforcementChange<Void> recorded = underTest.recordAttendance(CASE_REFERENCE, YesOrNo.NO);

        assertThat(recorded.errors()).containsExactly(EnforcementSideEffects.COULD_NOT_SCHEDULE_ERROR);
    }

    @Test
    void shouldRecordTheRiskAnswersThenTheAttendanceAnswer() {
        EnforcementOrderEntity enforcement = givenEnforcements(enforcementWithStatus(AWAITING_CLAIMANT_CONFIRMATION))
            .getFirst();
        EnforcementOrder submitted = EnforcementOrder.builder().claimantCanAttendEviction(YesOrNo.YES).build();
        when(claimantRiskAssessmentService.recordAnswers(CASE_REFERENCE, submitted))
            .thenReturn(EnforcementChange.made(true));

        assertThat(underTest.recordConfirmation(CASE_REFERENCE, YesOrNo.YES, submitted).refused()).isFalse();

        InOrder inOrder = inOrder(claimantRiskAssessmentService, enforcementOrderRepository);
        inOrder.verify(claimantRiskAssessmentService).recordAnswers(CASE_REFERENCE, submitted);
        inOrder.verify(enforcementOrderRepository).save(enforcement);
        assertThat(enforcement.getStatus()).isEqualTo(CLAIMANT_CONFIRMED);
    }

    /**
     * A refusal from the risk answers stops the attendance answer being recorded on top of answers that are
     * about to be rolled back.
     */
    @Test
    void shouldRefuseTheConfirmationWhenTheRiskAnswersAreRefused() {
        EnforcementOrder submitted = EnforcementOrder.builder().claimantCanAttendEviction(YesOrNo.YES).build();
        when(claimantRiskAssessmentService.recordAnswers(CASE_REFERENCE, submitted))
            .thenReturn(EnforcementChange.refused(EnforcementSideEffects.COULD_NOT_SCHEDULE_ERROR));

        EnforcementChange<Void> recorded = underTest.recordConfirmation(CASE_REFERENCE, YesOrNo.YES, submitted);

        assertThat(recorded.errors()).containsExactly(EnforcementSideEffects.COULD_NOT_SCHEDULE_ERROR);
        verifyNoInteractions(pcsCaseService, enforcementOrderRepository, enforcementNotificationService,
                             enforcementWaTaskService);
    }

    @Test
    void shouldMoveTheLiveEnforcementAndNotAClosedOne() {
        EnforcementOrderEntity closed = enforcementWithStatus(CLOSED);
        EnforcementOrderEntity live = enforcementWithStatus(AWAITING_CLAIMANT_CONFIRMATION);
        givenEnforcements(closed, live);

        underTest.recordAttendance(CASE_REFERENCE, YesOrNo.YES);

        assertThat(live.getStatus()).isEqualTo(CLAIMANT_CONFIRMED);
        assertThat(closed.getStatus()).isEqualTo(CLOSED);
        assertThat(closed.getStatusUpdatedAt()).isEqualTo(EARLIER);
        verify(enforcementOrderRepository).save(live);
    }

    @ParameterizedTest
    @EnumSource(value = EnforcementStatus.class, names = "AWAITING_CLAIMANT_CONFIRMATION",
        mode = EnumSource.Mode.EXCLUDE)
    void shouldNotChangeTheStatusWhenTheEnforcementIsNotAwaitingConfirmation(EnforcementStatus status) {
        EnforcementOrderEntity enforcement = givenEnforcements(enforcementWithStatus(status)).getFirst();

        String expectedError = status.isLive()
            ? ClaimantEvictionConfirmationService.EVICTION_CHANGED_ERROR
            : ClaimantEvictionConfirmationService.NO_LIVE_EVICTION_ERROR;

        assertThat(underTest.recordAttendance(CASE_REFERENCE, YesOrNo.YES).errors()).containsExactly(expectedError);
        assertThat(underTest.recordAttendance(CASE_REFERENCE, YesOrNo.NO).errors()).containsExactly(expectedError);

        assertThat(enforcement.getStatus()).isEqualTo(status);
        assertThat(enforcement.getStatusUpdatedAt()).isEqualTo(EARLIER);
        verify(enforcementOrderRepository, never()).save(any());
        verifyNoInteractions(enforcementNotificationService, enforcementWaTaskService);
    }

    @Test
    void shouldNotChangeAnythingWhenTheEnforcementHasNoStatus() {
        EnforcementOrderEntity enforcement = givenEnforcements(enforcementWithStatus(null)).getFirst();

        assertThat(underTest.recordAttendance(CASE_REFERENCE, YesOrNo.YES).errors())
            .containsExactly(ClaimantEvictionConfirmationService.NO_LIVE_EVICTION_ERROR);

        assertThat(enforcement.getStatus()).isNull();
        verify(enforcementOrderRepository, never()).save(any());
        verifyNoInteractions(enforcementNotificationService, enforcementWaTaskService);
    }

    @Test
    void shouldNotChangeAnythingWhenTheClaimHasNoEnforcement() {
        givenEnforcements();

        assertThat(underTest.recordAttendance(CASE_REFERENCE, YesOrNo.YES).errors())
            .containsExactly(ClaimantEvictionConfirmationService.NO_LIVE_EVICTION_ERROR);

        verify(enforcementOrderRepository, never()).save(any());
        verifyNoInteractions(enforcementNotificationService, enforcementWaTaskService);
    }

    @Test
    void shouldNotChangeAnythingWhenTheCaseHasNoClaim() {
        PcsCaseEntity pcsCase = new PcsCaseEntity();
        pcsCase.setClaims(List.of());
        when(pcsCaseService.loadCase(CASE_REFERENCE)).thenReturn(pcsCase);

        assertThat(underTest.recordAttendance(CASE_REFERENCE, YesOrNo.YES).errors())
            .containsExactly(ClaimantEvictionConfirmationService.NO_LIVE_EVICTION_ERROR);

        verifyNoInteractions(enforcementOrderRepository);
    }

    @Test
    void shouldNotChangeAnythingWhenThereIsNoAnswer() {
        assertThat(underTest.recordAttendance(CASE_REFERENCE, null).refused()).isFalse();

        verifyNoInteractions(pcsCaseService, enforcementOrderRepository, enforcementNotificationService,
                             enforcementWaTaskService);
    }

    private List<EnforcementOrderEntity> givenEnforcements(EnforcementOrderEntity... enforcements) {
        when(pcsCaseService.loadCase(CASE_REFERENCE)).thenReturn(buildPcsCaseEntity(UUID.randomUUID(), CLAIM_ID));
        List<EnforcementOrderEntity> list = List.of(enforcements);
        when(enforcementOrderRepository.findByClaimId(CLAIM_ID)).thenReturn(list);
        return list;
    }

    private static EnforcementOrderEntity enforcementWithStatus(EnforcementStatus status) {
        return EnforcementOrderEntity.builder()
            .id(UUID.randomUUID())
            .status(status)
            .statusUpdatedAt(status == null ? null : EARLIER)
            .bailiffDate(EVICTION)
            .build();
    }
}
