package uk.gov.hmcts.reform.pcs.ccd.event.service;

import com.fasterxml.jackson.databind.ObjectMapper;
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
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.ccd.sdk.SystemEventExecutor;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.reform.ccd.client.CoreCaseDataApi;
import uk.gov.hmcts.reform.pcs.ccd.event.EventId;
import uk.gov.hmcts.reform.pcs.ccd.event.action.ClaimPaymentAction;
import uk.gov.hmcts.reform.pcs.ccd.event.action.EnforcementPaymentAction;
import uk.gov.hmcts.reform.pcs.feesandpay.model.Payment;
import uk.gov.hmcts.reform.pcs.feesandpay.model.PaymentStateTaskData;
import uk.gov.hmcts.reform.pcs.feesandpay.model.PaymentStatusCallback;
import uk.gov.hmcts.reform.pcs.security.IdamTokenProvider;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.argumentSet;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static uk.gov.hmcts.reform.pcs.ccd.event.EventId.claimIssuePayment;
import static uk.gov.hmcts.reform.pcs.ccd.event.EventId.enforcementPayment;
import static uk.gov.hmcts.reform.pcs.feesandpay.task.CcdPaymentUpdateTaskComponent.CCD_PAYMENT_UPDATE_TASK_DESCRIPTOR;

@ExtendWith(MockitoExtension.class)
class CcdPaymentStateUpdateServiceTest {

    private static final long CASE_ID = 1234L;
    private static final String IDAM_BEARER = "Bearer idam";
    private static final String S2S_BEARER = "Bearer s2s";
    private UUID paymentReference;

    @Mock
    private IdamTokenProvider systemUpdateUserTokenProvider;
    @Mock
    private AuthTokenGenerator s2sAuthTokenGenerator;
    @Mock
    private CoreCaseDataApi coreCaseDataApi;
    @Mock
    private ObjectMapper objectMapper;
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

    @InjectMocks
    private CcdPaymentStateUpdateService underTest;

    @BeforeEach
    void beforeEach() {
        paymentReference = UUID.randomUUID();
    }

    @ParameterizedTest
    @MethodSource("eventIds")
    void shouldScheduleCcdPaymentUpdateTask(EventId eventId) {
        // Given
        Instant before = Instant.now();

        // When
        underTest.submitPaymentSuccess(CASE_ID, eventId, paymentStatusCallback());

        // Then
        Instant after = Instant.now();
        verify(schedulerClient).scheduleIfNotExists(schedulableInstanceCaptor.capture());

        SchedulableInstance<PaymentStateTaskData> scheduled = schedulableInstanceCaptor.getValue();
        TaskInstance<PaymentStateTaskData> taskInstance = scheduled.getTaskInstance();
        assertThat(scheduled.getNextExecutionTime(Instant.now())).isBetween(before, after);
        assertThat(taskInstance.getTaskName()).isEqualTo(CCD_PAYMENT_UPDATE_TASK_DESCRIPTOR.getTaskName());
        assertThat(taskInstance.getId()).isEqualTo(eventId + "-" + CASE_ID);
        assertThat(taskInstance.getData().getCaseReference()).isEqualTo(CASE_ID);
        assertThat(taskInstance.getData().getEventId()).isEqualTo(eventId);

    }

    @ParameterizedTest
    @MethodSource("eventIds")
    void shouldStartEventAndSubmitPaymentSuccessfully(EventId eventId) {
        // Given // When
        underTest.submitPaymentSuccess(CASE_ID, eventId, paymentStatusCallback());

        // Then
        verify(schedulerClient).scheduleIfNotExists(any());
    }

    @ParameterizedTest
    @MethodSource("eventIds")
    void shouldUseSameInstanceIdWhenSubmittedMultipleTimesForSamePayment(EventId eventId) {
        // Given // When
        underTest.submitPaymentSuccess(CASE_ID, eventId, paymentStatusCallback());
        underTest.submitPaymentSuccess(CASE_ID, eventId, paymentStatusCallback());

        // Then
        verify(schedulerClient, times(2)).scheduleIfNotExists(schedulableInstanceCaptor.capture());

        List<String> instanceIds = schedulableInstanceCaptor.getAllValues().stream()
            .map(scheduled -> scheduled.getTaskInstance().getId())
            .toList();

        assertThat(instanceIds).containsExactly(
            eventId.name() + "-" + CASE_ID,
            eventId.name() + "-" + CASE_ID
        );
    }

    @Test
    void shouldUseDifferentInstanceIdsForDifferentEvents() {
        // Given // When
        underTest.submitPaymentSuccess(CASE_ID, claimIssuePayment, paymentStatusCallback());
        underTest.submitPaymentSuccess(CASE_ID, enforcementPayment, paymentStatusCallback());

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
        PaymentStateTaskData taskData = PaymentStateTaskData.builder().eventId(eventId)
            .caseReference(CASE_ID).build();

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
