package uk.gov.hmcts.reform.pcs.ccd.event.service;

import com.github.kagkarlsson.scheduler.SchedulerClient;
import com.github.kagkarlsson.scheduler.task.SchedulableInstance;
import com.github.kagkarlsson.scheduler.task.TaskInstance;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.ccd.sdk.SystemEventExecutor;
import uk.gov.hmcts.reform.pcs.ccd.event.EventId;
import uk.gov.hmcts.reform.pcs.ccd.event.action.ClaimPaymentAction;
import uk.gov.hmcts.reform.pcs.ccd.event.action.EnforcementPaymentAction;
import uk.gov.hmcts.reform.pcs.feesandpay.model.Payment;
import uk.gov.hmcts.reform.pcs.feesandpay.model.PaymentStateTaskData;
import uk.gov.hmcts.reform.pcs.feesandpay.model.PaymentStatusCallback;
import uk.gov.hmcts.reform.pcs.system.SystemAction;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.argumentSet;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.pcs.ccd.event.EventId.claimIssuePayment;
import static uk.gov.hmcts.reform.pcs.ccd.event.EventId.enforcementPayment;
import static uk.gov.hmcts.reform.pcs.feesandpay.task.CcdPaymentUpdateTaskComponent.CCD_PAYMENT_UPDATE_TASK_DESCRIPTOR;
import static uk.gov.hmcts.reform.pcs.system.SystemAction.ENFORCEMENT_PAYMENT;

@ExtendWith(MockitoExtension.class)
class CcdPaymentStateUpdateServiceTest {

    private static final long CASE_ID = 1234L;
    private static final SystemAction systemAction = SystemAction.CLAIM_ISSUED_PAYMENT;
    private UUID paymentReference;

    @Mock
    private SchedulerClient schedulerClient;
    @Mock
    private SystemEventExecutor systemEventExecutor;
    @Mock
    private ClaimPaymentAction claimPaymentAction;
    @Mock
    private EnforcementPaymentAction enforcementPaymentAction;
    @Captor
    private ArgumentCaptor<SchedulableInstance<PaymentStateTaskData>> schedulableInstanceCaptor;

    private CcdPaymentStateUpdateService underTest;

    @BeforeEach
    void beforeEach() {
        paymentReference = UUID.randomUUID();
        lenient().when(claimPaymentAction.eventId()).thenReturn(claimIssuePayment);
        lenient().when(enforcementPaymentAction.eventId()).thenReturn(enforcementPayment);
        when(claimPaymentAction.systemAction()).thenReturn(systemAction);
        when(enforcementPaymentAction.systemAction()).thenReturn(ENFORCEMENT_PAYMENT);
        underTest = new CcdPaymentStateUpdateService(schedulerClient, systemEventExecutor,
                                                     List.of(claimPaymentAction, enforcementPaymentAction));
    }

    @ParameterizedTest
    @MethodSource("eventIds")
    void shouldScheduleCcdPaymentUpdateTask(EventId eventId) {
        // Given
        Instant before = Instant.now();
        PaymentStatusCallback paymentStatusCallback = paymentStatusCallback();

        // When
        underTest.submitPaymentSuccess(CASE_ID, eventId, paymentStatusCallback, systemAction);

        // Then
        Instant after = Instant.now();
        verify(schedulerClient).scheduleIfNotExists(schedulableInstanceCaptor.capture());

        SchedulableInstance<PaymentStateTaskData> scheduled = schedulableInstanceCaptor.getValue();
        TaskInstance<PaymentStateTaskData> taskInstance = scheduled.getTaskInstance();
        assertThat(scheduled.getNextExecutionTime(Instant.now())).isBetween(before, after);
        assertThat(taskInstance.getTaskName()).isEqualTo(CCD_PAYMENT_UPDATE_TASK_DESCRIPTOR.getTaskName());
        assertThat(taskInstance.getId()).isEqualTo(systemAction + "-" + paymentStatusCallback.getPaymentReference());
        assertThat(taskInstance.getData().getCaseReference()).isEqualTo(CASE_ID);
        assertThat(taskInstance.getData().getSystemAction()).isEqualTo(systemAction);
        assertThat(taskInstance.getData().getEventId()).isEqualTo(eventId);
    }

    @ParameterizedTest
    @MethodSource("eventIds")
    void shouldStartEventAndSubmitPaymentSuccessfully(EventId eventId) {
        // Given // When
        underTest.submitPaymentSuccess(CASE_ID, eventId, paymentStatusCallback(), systemAction);

        // Then
        verify(schedulerClient).scheduleIfNotExists(any());
    }

    @ParameterizedTest
    @MethodSource("eventIds")
    void shouldUseSameInstanceIdWhenSubmittedMultipleTimesForSamePayment(EventId eventId) {
        // Given // When
        PaymentStatusCallback paymentStatusCallback = paymentStatusCallback();
        underTest.submitPaymentSuccess(CASE_ID, eventId, paymentStatusCallback, systemAction);
        underTest.submitPaymentSuccess(CASE_ID, eventId, paymentStatusCallback, systemAction);

        // Then
        verify(schedulerClient, times(2)).scheduleIfNotExists(schedulableInstanceCaptor.capture());

        List<String> instanceIds = schedulableInstanceCaptor.getAllValues().stream()
            .map(scheduled -> scheduled.getTaskInstance().getId())
            .toList();

        assertThat(instanceIds).containsExactly(
            systemAction.name() + "-" + paymentStatusCallback.getPaymentReference(),
            systemAction.name() + "-" + paymentStatusCallback.getPaymentReference()
        );
    }

    @Test
    void shouldUseDifferentInstanceIdsForDifferentEvents() {
        // Given // When
        underTest.submitPaymentSuccess(CASE_ID, claimIssuePayment, paymentStatusCallback(), systemAction);
        underTest.submitPaymentSuccess(CASE_ID, enforcementPayment, paymentStatusCallback(), ENFORCEMENT_PAYMENT);

        // Then
        verify(schedulerClient, times(2)).scheduleIfNotExists(schedulableInstanceCaptor.capture());

        List<String> instanceIds = schedulableInstanceCaptor.getAllValues().stream()
            .map(scheduled -> scheduled.getTaskInstance().getId())
            .toList();

        assertThat(instanceIds).doesNotHaveDuplicates();
    }

    @ParameterizedTest
    @MethodSource("eventIds")
    void shouldUpdateCcd(EventId eventId) {
        // Given
        PaymentStateTaskData taskData = PaymentStateTaskData.builder().eventId(eventId).caseReference(CASE_ID).build();

        // When
        underTest.updateCcd(taskData);

        // Then
        verify(systemEventExecutor).execute(anyLong(), any(), any());
    }

    private static Stream<Arguments> eventIds() {
        return Stream.of(
            argumentSet(claimIssuePayment.name(), claimIssuePayment),
            argumentSet(enforcementPayment.name(), enforcementPayment)
        );
    }

    private PaymentStatusCallback paymentStatusCallback() {
        return PaymentStatusCallback.builder().payment(Payment.builder()
            .paymentReference(paymentReference.toString()).build()).build();
    }

}
