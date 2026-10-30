package uk.gov.hmcts.reform.pcs.ccd.event.action;

import com.github.kagkarlsson.scheduler.SchedulerClient;
import com.github.kagkarlsson.scheduler.task.SchedulableInstance;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.ccd.sdk.SystemEventResult;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.event.EventId;
import uk.gov.hmcts.reform.pcs.ccd.model.AccessCodeTaskData;
import uk.gov.hmcts.reform.pcs.ccd.service.DefendantAccessCodeService;
import uk.gov.hmcts.reform.pcs.ccd.service.PcsCaseService;
import uk.gov.hmcts.reform.pcs.ccd.service.claimform.ClaimFormScheduler;
import uk.gov.hmcts.reform.pcs.feesandpay.model.PaymentStateTaskData;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.pcs.ccd.task.AccessCodeGenerationComponent.ACCESS_CODE_TASK_DESCRIPTOR;

@ExtendWith(MockitoExtension.class)
class ClaimPaymentActionTest {

    private static final long CASE_REFERENCE = 1111222233334444L;
    private static final String PAYMENT_REFERENCE = "RC-1111-2222-3333-4444";

    @Mock
    private PcsCaseService pcsCaseService;
    @Mock
    private ClaimFormScheduler claimFormScheduler;
    @Mock
    private DefendantAccessCodeService defendantAccessCodeService;
    @Mock
    private SchedulerClient schedulerClient;
    @Mock
    private PcsCaseEntity pcsCaseEntity;

    @Captor
    private ArgumentCaptor<SchedulableInstance<AccessCodeTaskData>> schedulableInstanceCaptor;

    private ClaimPaymentAction underTest;

    @BeforeEach
    void setUp() {
        underTest = new ClaimPaymentAction(pcsCaseService, claimFormScheduler, defendantAccessCodeService,
                                           schedulerClient);
    }

    @Test
    void shouldReturnClaimIssuePaymentEventId() {
        // Given ... When ...
        EventId eventId = underTest.eventId();

        // Then
        assertThat(eventId).isEqualTo(EventId.claimIssuePayment);
    }

    @Test
    void shouldIssueCaseAndScheduleClaimFormWhenClaimNotYetIssued() {
        // Given
        givenCaseWithClaimIssuedDate(null);
        when(defendantAccessCodeService.findDefendantPartyIdsNeedingAccessCode(CASE_REFERENCE)).thenReturn(List.of());

        // When
        underTest.apply(taskData());

        // Then
        verify(pcsCaseService).setCaseIssuedDate(CASE_REFERENCE);
        verify(claimFormScheduler).scheduleClaimFormGeneration(CASE_REFERENCE);
    }

    @Test
    void shouldScheduleOneAccessCodeTaskPerDefendantWhenClaimNotYetIssued() {
        // Given
        UUID firstDefendantId = UUID.randomUUID();
        UUID secondDefendantId = UUID.randomUUID();
        givenCaseWithClaimIssuedDate(null);
        when(defendantAccessCodeService.findDefendantPartyIdsNeedingAccessCode(CASE_REFERENCE))
            .thenReturn(List.of(firstDefendantId, secondDefendantId));
        Instant before = Instant.now();

        // When
        underTest.apply(taskData());

        // Then
        Instant after = Instant.now();
        verify(schedulerClient, org.mockito.Mockito.times(2))
            .scheduleIfNotExists(schedulableInstanceCaptor.capture());
        List<SchedulableInstance<AccessCodeTaskData>> scheduled = schedulableInstanceCaptor.getAllValues();
        assertThat(scheduled).hasSize(2);
        assertScheduledForDefendant(scheduled.get(0), firstDefendantId, before, after);
        assertScheduledForDefendant(scheduled.get(1), secondDefendantId, before, after);
    }

    @SuppressWarnings("unchecked")
    @Test
    void shouldNotScheduleAccessCodeTasksWhenNoDefendantsNeedAccessCode() {
        // Given
        givenCaseWithClaimIssuedDate(null);
        when(defendantAccessCodeService.findDefendantPartyIdsNeedingAccessCode(CASE_REFERENCE))
            .thenReturn(List.of());

        // When
        underTest.apply(taskData());

        // Then
        verify(schedulerClient, never()).scheduleIfNotExists(any(SchedulableInstance.class));
    }

    @Test
    void shouldNotIssueCaseOrScheduleAnyTasksWhenClaimAlreadyIssued() {
        // Given
        givenCaseWithClaimIssuedDate(LocalDateTime.now().minusDays(1));

        // When
        underTest.apply(taskData());

        // Then
        verify(pcsCaseService, never()).setCaseIssuedDate(any(Long.class));
        verifyNoInteractions(claimFormScheduler, defendantAccessCodeService, schedulerClient);
    }

    @Test
    void shouldTransitionToCaseIssuedStateWhenClaimNotYetIssued() {
        // Given
        givenCaseWithClaimIssuedDate(null);
        when(defendantAccessCodeService.findDefendantPartyIdsNeedingAccessCode(CASE_REFERENCE))
            .thenReturn(List.of());

        // When
        SystemEventResult result = underTest.apply(taskData());

        // Then
        assertThat(result.state()).isPresent();
        Enum<?> state = result.state().get();
        assertThat(state).isEqualTo(State.CASE_ISSUED);
    }

    @Test
    void shouldStillTransitionToCaseIssuedStateWhenClaimAlreadyIssued() {
        // Given
        givenCaseWithClaimIssuedDate(LocalDateTime.now().minusDays(1));

        // When
        SystemEventResult result = underTest.apply(taskData());

        // Then
        assertThat(result.state()).isPresent();
        Enum<?> state = result.state().get();
        assertThat(state).isEqualTo(State.CASE_ISSUED);
    }

    @Test
    void shouldUseTaskDataEventIdAsEventIdAndEventName() {
        // Given
        givenCaseWithClaimIssuedDate(LocalDateTime.now().minusDays(1));

        // When
        SystemEventResult result = underTest.apply(taskData());

        // Then
        assertThat(result.eventId()).isEqualTo(EventId.claimIssuePayment.name());
        assertThat(result.eventName()).isEqualTo(EventId.claimIssuePayment.name());
    }

    @Test
    void shouldBuildSummaryFromPaymentAndCaseReferences() {
        // Given
        givenCaseWithClaimIssuedDate(LocalDateTime.now().minusDays(1));

        // When
        SystemEventResult result = underTest.apply(taskData());

        // Then
        assertThat(result.summary())
            .contains("Payment " + PAYMENT_REFERENCE + " updated for " + CASE_REFERENCE);
    }

    private void assertScheduledForDefendant(SchedulableInstance<AccessCodeTaskData> scheduled, UUID defendantPartyId,
                                             Instant earliest, Instant latest) {
        assertThat(scheduled.getTaskInstance().getTaskName()).isEqualTo(ACCESS_CODE_TASK_DESCRIPTOR.getTaskName());
        assertThat(scheduled.getTaskInstance().getId()).isEqualTo(CASE_REFERENCE + ":" + defendantPartyId);

        AccessCodeTaskData data = scheduled.getTaskInstance().getData();
        assertThat(data.getCaseReference()).isEqualTo(String.valueOf(CASE_REFERENCE));
        assertThat(data.getDefendantPartyId()).isEqualTo(defendantPartyId.toString());

        Instant executionTime = scheduled.getNextExecutionTime(Instant.now());
        assertThat(executionTime)
            .isBetween(earliest.minus(Duration.ofSeconds(1)), latest.plus(Duration.ofSeconds(1)));
    }

    private void givenCaseWithClaimIssuedDate(LocalDateTime claimIssuedDate) {
        ClaimEntity claimEntity = ClaimEntity.builder().claimIssuedDate(claimIssuedDate).build();
        List<ClaimEntity> claims = new ArrayList<>(List.of(claimEntity));

        when(pcsCaseService.loadCase(CASE_REFERENCE)).thenReturn(pcsCaseEntity);
        when(pcsCaseEntity.getClaims()).thenReturn(claims);
    }

    private PaymentStateTaskData taskData() {
        return PaymentStateTaskData.builder().caseReference(CASE_REFERENCE).eventId(EventId.claimIssuePayment)
            .paymentReference(PAYMENT_REFERENCE).idempotencyKey(UUID.randomUUID()).build();
    }

}
