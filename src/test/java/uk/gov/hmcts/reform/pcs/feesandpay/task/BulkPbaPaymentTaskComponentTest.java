package uk.gov.hmcts.reform.pcs.feesandpay.task;

import com.github.kagkarlsson.scheduler.task.CompletionHandler;
import com.github.kagkarlsson.scheduler.task.Execution;
import com.github.kagkarlsson.scheduler.task.ExecutionComplete;
import com.github.kagkarlsson.scheduler.task.ExecutionContext;
import com.github.kagkarlsson.scheduler.task.ExecutionOperations;
import com.github.kagkarlsson.scheduler.task.TaskInstance;
import com.github.kagkarlsson.scheduler.task.helper.CustomTask;
import feign.FeignException;
import feign.Request;
import feign.RetryableException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.payments.client.InvalidPaymentRequestException;
import uk.gov.hmcts.reform.pcs.feesandpay.model.BulkPbaPaymentTaskData;
import uk.gov.hmcts.reform.pcs.feesandpay.service.BulkPaymentStatusListener;
import uk.gov.hmcts.reform.pcs.feesandpay.service.PaymentService;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class BulkPbaPaymentTaskComponentTest {

    private static final String SERVICE_REQUEST = "2025-1234";
    private static final int MAX_RETRIES = 3;
    private static final UUID SUBMISSION_ID = UUID.randomUUID();

    @Mock
    private PaymentService paymentService;
    @Mock
    private BulkPaymentStatusListener listener;
    @Mock
    private TaskInstance<BulkPbaPaymentTaskData> taskInstance;
    @Mock
    private ExecutionContext executionContext;
    @Mock
    private ExecutionOperations<BulkPbaPaymentTaskData> operations;

    private CustomTask<BulkPbaPaymentTaskData> task;
    private BulkPbaPaymentTaskData data;

    @BeforeEach
    void setUp() {
        task = new BulkPbaPaymentTaskComponent(paymentService, listener, MAX_RETRIES, Duration.ofMinutes(1))
            .bulkPbaPaymentTask();
        data = BulkPbaPaymentTaskData.builder()
            .serviceRequestReference(SERVICE_REQUEST)
            .caseReference(1234L)
            .pbaAccountNumber("PBA0123456")
            .bulkClaimSubmissionId(SUBMISSION_ID)
            .amount(new BigDecimal("123.40"))
            .build();
        lenient().when(taskInstance.getData()).thenReturn(data);
    }

    @Test
    void shouldHaveExpectedDescriptor() {
        assertThat(BulkPbaPaymentTaskComponent.BULK_PBA_PAYMENT_TASK_DESCRIPTOR.getTaskName())
            .isEqualTo("bulk-pba-payment-task");
    }

    @Test
    void shouldNotifySubmittedOnSuccess() {
        CompletionHandler<BulkPbaPaymentTaskData> result = task.execute(taskInstance, executionContext);

        assertThat(result).isInstanceOf(CompletionHandler.OnCompleteRemove.class);
        verify(listener).onPbaPaymentSubmitted(SUBMISSION_ID, SERVICE_REQUEST);
        verify(listener, never()).onPbaPaymentFailed(any(), anyString(), anyString());
    }

    @Test
    void shouldTreatDuplicatePaymentAsSuccess() {
        doThrow(new InvalidPaymentRequestException("Duplicate Payment")).when(paymentService)
            .createSystemPbaPayment(data);

        CompletionHandler<BulkPbaPaymentTaskData> result = task.execute(taskInstance, executionContext);

        assertThat(result).isInstanceOf(CompletionHandler.OnCompleteRemove.class);
        verify(listener).onPbaPaymentSubmitted(SUBMISSION_ID, SERVICE_REQUEST);
    }

    @Test
    void shouldFailFinallyOnPaymentRequiredWithoutRetry() {
        doThrow(feignException(402)).when(paymentService).createSystemPbaPayment(data);

        CompletionHandler<BulkPbaPaymentTaskData> result = task.execute(taskInstance, executionContext);

        assertThat(result).isInstanceOf(CompletionHandler.OnCompleteRemove.class);
        verify(listener).onPbaPaymentFailed(eq(SUBMISSION_ID), eq(SERVICE_REQUEST), anyString());
        verify(listener, never()).onPbaPaymentSubmitted(any(), anyString());
    }

    @Test
    void shouldFailFinallyOnOtherClientErrors() {
        doThrow(feignException(403)).when(paymentService).createSystemPbaPayment(data);

        CompletionHandler<BulkPbaPaymentTaskData> result = task.execute(taskInstance, executionContext);

        assertThat(result).isInstanceOf(CompletionHandler.OnCompleteRemove.class);
        verify(listener).onPbaPaymentFailed(eq(SUBMISSION_ID), eq(SERVICE_REQUEST), anyString());
    }

    @Test
    void shouldRethrowServerErrorsWithoutNotifying() {
        FeignException serverError = feignException(503);
        doThrow(serverError).when(paymentService).createSystemPbaPayment(data);

        assertThatThrownBy(() -> task.execute(taskInstance, executionContext)).isSameAs(serverError);
        verifyNoInteractions(listener);
    }

    @Test
    void shouldRethrowIoErrorsWithoutNotifying() {
        Request request = request();
        RetryableException ioError = new RetryableException(
            -1, "timeout", Request.HttpMethod.POST, (Long) null, request);
        doThrow(ioError).when(paymentService).createSystemPbaPayment(data);

        assertThatThrownBy(() -> task.execute(taskInstance, executionContext)).isSameAs(ioError);
        verifyNoInteractions(listener);
    }

    @Test
    void shouldRescheduleWithBackoffWhileRetriesRemain() {

        task.getFailureHandler().onFailure(executionComplete(1), operations);

        verify(operations).reschedule(any(ExecutionComplete.class), any(Instant.class));
        verify(operations, never()).remove();
        verifyNoInteractions(listener);
    }

    @Test
    void shouldNotifyFailedAndRemoveOnceRetriesExhausted() {

        task.getFailureHandler().onFailure(executionComplete(MAX_RETRIES), operations);

        verify(operations).remove();
        verify(listener).onPbaPaymentFailed(eq(SUBMISSION_ID), eq(SERVICE_REQUEST), anyString());
    }

    private ExecutionComplete executionComplete(int consecutiveFailures) {
        Execution execution = new Execution(Instant.now(), taskInstance, false, null, null, null, 
                                            consecutiveFailures, null, 1);
        return ExecutionComplete.failure(execution, Instant.now(), Instant.now(), new RuntimeException("x"));
    }

    private static Request request() {
        return Request.create(Request.HttpMethod.POST, "http://payments/pba", Collections.emptyMap(),
                              new byte[0], StandardCharsets.UTF_8, null);
    }

    private static FeignException feignException(int status) {
        return FeignException.errorStatus("createPbaPayment", feign.Response.builder()
            .status(status).reason("r").request(request()).headers(Collections.emptyMap())
            .body("{}", StandardCharsets.UTF_8).build());
    }

}
