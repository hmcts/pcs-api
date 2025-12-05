package uk.gov.hmcts.reform.pcs.feesandpay.task;

import com.github.kagkarlsson.scheduler.task.CompletionHandler;
import com.github.kagkarlsson.scheduler.task.CompletionHandler.OnCompleteRemove;
import com.github.kagkarlsson.scheduler.task.Execution;
import com.github.kagkarlsson.scheduler.task.ExecutionContext;
import com.github.kagkarlsson.scheduler.task.FailureHandler;
import com.github.kagkarlsson.scheduler.task.TaskInstance;
import com.github.kagkarlsson.scheduler.task.helper.CustomTask;
import feign.FeignException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import uk.gov.hmcts.reform.pcs.feesandpay.model.FeeDetails;
import uk.gov.hmcts.reform.pcs.feesandpay.model.FeeTypes;
import uk.gov.hmcts.reform.pcs.feesandpay.model.FeesAndPayTaskData;
import uk.gov.hmcts.reform.pcs.feesandpay.service.FeeService;
import uk.gov.hmcts.reform.pcs.feesandpay.service.PaymentService;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyInt;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.pcs.feesandpay.task.FeesAndPayTaskComponent.FEE_CASE_ISSUED_TASK_DESCRIPTOR;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("FeesAndPayTaskComponent Tests")
class FeesAndPayTaskComponentTest {

    private FeesAndPayTaskComponent feesAndPayTaskComponent;

    @Mock
    private FeeService feeService;

    @Mock
    private PaymentService paymentService;

    @Mock
    private TaskInstance<FeesAndPayTaskData> taskInstance;

    @Mock
    private ExecutionContext executionContext;

    @Mock
    private Execution execution;

    private final Duration feesAndPayBackoffDelay = Duration.ofMinutes(5);

    private static final String TASK_ID = "fee-task-123";

    @BeforeEach
    void setUp() {
        int maxRetriesFeesAndPay = 5;
        feesAndPayTaskComponent = new FeesAndPayTaskComponent(
            feeService,
            paymentService,
            maxRetriesFeesAndPay,
            feesAndPayBackoffDelay
        );

        when(taskInstance.getId()).thenReturn(TASK_ID);
        when(executionContext.getExecution()).thenReturn(execution);
    }

    private FeesAndPayTaskData buildTaskData(String feeType, FeeDetails feeDetails) {
        return FeesAndPayTaskData.builder()
            .feeType(feeType)
            .feeDetails(feeDetails)
            .caseReference("BUS-123")
            .ccdCaseNumber("1111-2222-3333-4444")
            .volume(2)
            .responsibleParty("Applicant")
            .build();
    }

    @Nested
    @DisplayName("Component Initialization Tests")
    class ComponentInitializationTests {

        @Test
        @DisplayName("Should create task descriptor with correct name and type")
        void shouldCreateTaskDescriptorWithCorrectNameAndType() {
            assertThat(FEE_CASE_ISSUED_TASK_DESCRIPTOR.getTaskName())
                .isEqualTo("fees-and-pay-task");
            assertThat(FEE_CASE_ISSUED_TASK_DESCRIPTOR.getDataClass())
                .isEqualTo(FeesAndPayTaskData.class);
        }

        @Test
        @DisplayName("Should create fees and pay task bean")
        void shouldCreateFeesAndPayTaskBean() {
            CustomTask<FeesAndPayTaskData> task = feesAndPayTaskComponent.feePaymentTask();
            assertThat(task).isNotNull();
        }
    }

    @Nested
    @DisplayName("Successful Flow Tests")
    class SuccessfulFlowTests {

        @Test
        @DisplayName("Should create service request with fee details")
        void shouldCreateServiceRequestWithFeeDetails() {
            FeeDetails feeDetails = mock(FeeDetails.class);
            FeesAndPayTaskData data = buildTaskData("some fee type", feeDetails);
            when(taskInstance.getData()).thenReturn(data);

            CustomTask<FeesAndPayTaskData> task = feesAndPayTaskComponent.feePaymentTask();

            CompletionHandler<FeesAndPayTaskData> result = task.execute(taskInstance, executionContext);

            verify(paymentService).createServiceRequest(
                data.getCaseReference(),
                data.getCcdCaseNumber(),
                feeDetails,
                data.getVolume(),
                data.getResponsibleParty()
            );
            assertThat(result).isInstanceOf(OnCompleteRemove.class);
        }
    }

    @Nested
    @DisplayName("Failure Handling Tests")
    class FailureHandlingTests {

        @Test
        @DisplayName("Should rethrow exception when payment service call fails")
        void shouldThrowFeeNotFoundExceptionWhenApiCallFails() {
            FeeDetails feeDetails = mock(FeeDetails.class);
            FeesAndPayTaskData data = buildTaskData(FeeTypes.CASE_ISSUE_FEE.getCode(), feeDetails);
            when(taskInstance.getData()).thenReturn(data);

            FeignException exception = mock(FeignException.class);
            when(paymentService.createServiceRequest(anyString(), anyString(), any(FeeDetails.class),
                                                     anyInt(), anyString()
            )).thenThrow(exception);

            CustomTask<FeesAndPayTaskData> task = feesAndPayTaskComponent.feePaymentTask();

            assertThatThrownBy(() -> task.execute(taskInstance, executionContext))
                .isEqualTo(exception);
        }
    }

    @Nested
    @DisplayName("Task Configuration Tests")
    class TaskConfigurationTests {

        @Test
        @DisplayName("Should configure task with correct failure handlers")
        void shouldConfigureTaskWithCorrectFailureHandlers() {
            CustomTask<FeesAndPayTaskData> task = feesAndPayTaskComponent.feePaymentTask();
            FailureHandler<FeesAndPayTaskData> failureHandler = task.getFailureHandler();
            assertThat(failureHandler).isInstanceOf(FailureHandler.MaxRetriesFailureHandler.class);
        }
    }
}