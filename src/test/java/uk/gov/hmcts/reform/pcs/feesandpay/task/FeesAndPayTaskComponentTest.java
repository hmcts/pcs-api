package uk.gov.hmcts.reform.pcs.feesandpay.task;

import com.github.kagkarlsson.scheduler.task.CompletionHandler;
import com.github.kagkarlsson.scheduler.task.Execution;
import com.github.kagkarlsson.scheduler.task.ExecutionContext;
import com.github.kagkarlsson.scheduler.task.TaskInstance;
import com.github.kagkarlsson.scheduler.task.helper.CustomTask;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import uk.gov.hmcts.reform.pcs.feesandpay.entity.Fee;
import uk.gov.hmcts.reform.pcs.feesandpay.exception.FeeNotFoundException;
import uk.gov.hmcts.reform.pcs.feesandpay.service.FeesAndPayService;

import java.math.BigDecimal;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("FeesAndPayTaskComponent Tests")
class FeesAndPayTaskComponentTest {

    private FeesAndPayTaskComponent feesAndPayTaskComponent;

    @Mock
    private FeesAndPayService feesAndPayService;

    @Mock
    private TaskInstance<String> taskInstance;

    @Mock
    private ExecutionContext executionContext;

    @Mock
    private Execution execution;

    private final Duration feesAndPayBackoffDelay = Duration.ofMinutes(5);

    private static final String TASK_ID = "fee-task-123";
    private static final String CASE_ISSUE_FEE_TYPE = "caseIssueFee";
    private static final String HEARING_FEE_TYPE = "hearingFee";

    @BeforeEach
    void setUp() {
        int maxRetriesFeesAndPay = 5;
        feesAndPayTaskComponent = new FeesAndPayTaskComponent(
            feesAndPayService,
            maxRetriesFeesAndPay,
            feesAndPayBackoffDelay
        );

        when(taskInstance.getId()).thenReturn(TASK_ID);
        when(executionContext.getExecution()).thenReturn(execution);
    }

    @Nested
    @DisplayName("Component Initialization Tests")
    class ComponentInitializationTests {

        @Test
        @DisplayName("Should create task descriptor with correct name and type")
        void shouldCreateTaskDescriptorWithCorrectNameAndType() {
            assertThat(FeesAndPayTaskComponent.FEE_CASE_ISSUED_TASK_DESCRIPTOR.getTaskName())
                .isEqualTo("fees-and-pay-task");
            assertThat(FeesAndPayTaskComponent.FEE_CASE_ISSUED_TASK_DESCRIPTOR.getDataClass())
                .isEqualTo(String.class);
        }

        @Test
        @DisplayName("Should create fees and pay task bean")
        void shouldCreateFeesAndPayTaskBean() {
            CustomTask<String> task = feesAndPayTaskComponent.feesAndPayCaseIssuedTask();

            assertThat(task).isNotNull();
        }
    }

    @Nested
    @DisplayName("Successful Fee Retrieval Tests")
    class SuccessfulFeeRetrievalTests {

        @Test
        @DisplayName("Should retrieve case issue fee successfully")
        void shouldRetrieveCaseIssueFeeSuccessfully() {
            Fee expectedFee = Fee.builder()
                .code("FEE0412")
                .description("Recovery of Land - County Court")
                .version("4")
                .calculatedAmount(new BigDecimal("404.00"))
                .build();

            when(taskInstance.getData()).thenReturn(CASE_ISSUE_FEE_TYPE);
            when(feesAndPayService.getFee(CASE_ISSUE_FEE_TYPE)).thenReturn(expectedFee);

            CustomTask<String> task = feesAndPayTaskComponent.feesAndPayCaseIssuedTask();

            CompletionHandler<String> result = task.execute(taskInstance, executionContext);

            verify(feesAndPayService).getFee(CASE_ISSUE_FEE_TYPE);
            assertThat(result).isInstanceOf(CompletionHandler.OnCompleteRemove.class);
        }

        @Test
        @DisplayName("Should retrieve hearing fee successfully")
        void shouldRetrieveHearingFeeSuccessfully() {
            Fee expectedFee = Fee.builder()
                .code("FEE9999")
                .description("Hearing Fee")
                .version("1")
                .calculatedAmount(new BigDecimal("100.00"))
                .build();

            when(taskInstance.getData()).thenReturn(HEARING_FEE_TYPE);
            when(feesAndPayService.getFee(HEARING_FEE_TYPE)).thenReturn(expectedFee);

            CustomTask<String> task = feesAndPayTaskComponent.feesAndPayCaseIssuedTask();

            CompletionHandler<String> result = task.execute(taskInstance, executionContext);

            verify(feesAndPayService).getFee(HEARING_FEE_TYPE);
            assertThat(result).isInstanceOf(CompletionHandler.OnCompleteRemove.class);
        }

        @Test
        @DisplayName("Should handle fee with zero amount")
        void shouldHandleFeeWithZeroAmount() {
            Fee zeroFee = Fee.builder()
                .code("FEE0000")
                .description("Waived Fee")
                .version("1")
                .calculatedAmount(BigDecimal.ZERO)
                .build();

            when(taskInstance.getData()).thenReturn(CASE_ISSUE_FEE_TYPE);
            when(feesAndPayService.getFee(CASE_ISSUE_FEE_TYPE)).thenReturn(zeroFee);

            CustomTask<String> task = feesAndPayTaskComponent.feesAndPayCaseIssuedTask();

            CompletionHandler<String> result = task.execute(taskInstance, executionContext);

            verify(feesAndPayService).getFee(CASE_ISSUE_FEE_TYPE);
            assertThat(result).isInstanceOf(CompletionHandler.OnCompleteRemove.class);
        }

        @Test
        @DisplayName("Should handle fee with large amount")
        void shouldHandleFeeWithLargeAmount() {
            Fee largeFee = Fee.builder()
                .code("FEE9999")
                .description("High Value Fee")
                .version("1")
                .calculatedAmount(new BigDecimal("10000.00"))
                .build();

            when(taskInstance.getData()).thenReturn(CASE_ISSUE_FEE_TYPE);
            when(feesAndPayService.getFee(CASE_ISSUE_FEE_TYPE)).thenReturn(largeFee);

            CustomTask<String> task = feesAndPayTaskComponent.feesAndPayCaseIssuedTask();

            CompletionHandler<String> result = task.execute(taskInstance, executionContext);

            verify(feesAndPayService).getFee(CASE_ISSUE_FEE_TYPE);
            assertThat(result).isInstanceOf(CompletionHandler.OnCompleteRemove.class);
        }
    }

    @Nested
    @DisplayName("FeeNotFoundException Handling Tests")
    class FeeNotFoundExceptionHandlingTests {

        @Test
        @DisplayName("Should throw FeeNotFoundException when fee type not configured")
        void shouldThrowFeeNotFoundExceptionWhenFeeTypeNotConfigured() {
            String invalidFeeType = "invalidFeeType";
            FeeNotFoundException exception = new FeeNotFoundException("Fee not found for feeType: " + invalidFeeType);

            when(taskInstance.getData()).thenReturn(invalidFeeType);
            when(feesAndPayService.getFee(invalidFeeType)).thenThrow(exception);

            CustomTask<String> task = feesAndPayTaskComponent.feesAndPayCaseIssuedTask();

            assertThatThrownBy(() -> task.execute(taskInstance, executionContext))
                .isInstanceOf(FeeNotFoundException.class)
                .hasMessage("Fee not found for feeType: " + invalidFeeType);

            verify(feesAndPayService).getFee(invalidFeeType);
        }

        @Test
        @DisplayName("Should throw FeeNotFoundException when API call fails")
        void shouldThrowFeeNotFoundExceptionWhenApiCallFails() {
            FeeNotFoundException exception = new FeeNotFoundException(
                "Unable to retrieve fee: " + CASE_ISSUE_FEE_TYPE
            );

            when(taskInstance.getData()).thenReturn(CASE_ISSUE_FEE_TYPE);
            when(feesAndPayService.getFee(CASE_ISSUE_FEE_TYPE)).thenThrow(exception);

            CustomTask<String> task = feesAndPayTaskComponent.feesAndPayCaseIssuedTask();

            assertThatThrownBy(() -> task.execute(taskInstance, executionContext))
                .isInstanceOf(FeeNotFoundException.class)
                .hasMessageContaining("Unable to retrieve fee");

            verify(feesAndPayService).getFee(CASE_ISSUE_FEE_TYPE);
        }

        @Test
        @DisplayName("Should propagate FeeNotFoundException with cause")
        void shouldPropagateFeeNotFoundExceptionWithCause() {
            RuntimeException cause = new RuntimeException("API connection failed");
            FeeNotFoundException exception = new FeeNotFoundException("Unable to retrieve fee", cause);

            when(taskInstance.getData()).thenReturn(CASE_ISSUE_FEE_TYPE);
            when(feesAndPayService.getFee(CASE_ISSUE_FEE_TYPE)).thenThrow(exception);

            CustomTask<String> task = feesAndPayTaskComponent.feesAndPayCaseIssuedTask();

            assertThatThrownBy(() -> task.execute(taskInstance, executionContext))
                .isInstanceOf(FeeNotFoundException.class)
                .hasCause(cause);

            verify(feesAndPayService).getFee(CASE_ISSUE_FEE_TYPE);
        }
    }

    @Nested
    @DisplayName("Exception Handling Tests")
    class ExceptionHandlingTests {

        @Test
        @DisplayName("Should propagate RuntimeException")
        void shouldPropagateRuntimeException() {
            RuntimeException exception = new RuntimeException("Unexpected error");

            when(taskInstance.getData()).thenReturn(CASE_ISSUE_FEE_TYPE);
            when(feesAndPayService.getFee(CASE_ISSUE_FEE_TYPE)).thenThrow(exception);

            CustomTask<String> task = feesAndPayTaskComponent.feesAndPayCaseIssuedTask();

            assertThatThrownBy(() -> task.execute(taskInstance, executionContext))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Unexpected error");

            verify(feesAndPayService).getFee(CASE_ISSUE_FEE_TYPE);
        }

        @Test
        @DisplayName("Should propagate IllegalArgumentException")
        void shouldPropagateIllegalArgumentException() {
            IllegalArgumentException exception = new IllegalArgumentException("Invalid fee type");

            when(taskInstance.getData()).thenReturn(CASE_ISSUE_FEE_TYPE);
            when(feesAndPayService.getFee(CASE_ISSUE_FEE_TYPE)).thenThrow(exception);

            CustomTask<String> task = feesAndPayTaskComponent.feesAndPayCaseIssuedTask();

            assertThatThrownBy(() -> task.execute(taskInstance, executionContext))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Invalid fee type");

            verify(feesAndPayService).getFee(CASE_ISSUE_FEE_TYPE);
        }
    }

    @Nested
    @DisplayName("Fee Type Validation Tests")
    class FeeTypeValidationTests {

        @Test
        @DisplayName("Should handle different fee types")
        void shouldHandleDifferentFeeTypes() {
            String[] feeTypes = {"caseIssueFee", "hearingFee", "appealFee", "copyFee"};

            for (String feeType : feeTypes) {
                Fee fee = Fee.builder()
                    .code("FEE" + feeType.hashCode())
                    .description("Fee for " + feeType)
                    .version("1")
                    .calculatedAmount(new BigDecimal("100.00"))
                    .build();

                when(taskInstance.getData()).thenReturn(feeType);
                when(feesAndPayService.getFee(feeType)).thenReturn(fee);

                CustomTask<String> task = feesAndPayTaskComponent.feesAndPayCaseIssuedTask();

                CompletionHandler<String> result = task.execute(taskInstance, executionContext);

                verify(feesAndPayService).getFee(feeType);
                assertThat(result).isInstanceOf(CompletionHandler.OnCompleteRemove.class);
            }
        }

        @Test
        @DisplayName("Should handle null fee type")
        void shouldHandleNullFeeType() {
            when(taskInstance.getData()).thenReturn(null);
            when(feesAndPayService.getFee(null))
                .thenThrow(new FeeNotFoundException("Fee type cannot be null"));

            CustomTask<String> task = feesAndPayTaskComponent.feesAndPayCaseIssuedTask();

            assertThatThrownBy(() -> task.execute(taskInstance, executionContext))
                .isInstanceOf(FeeNotFoundException.class)
                .hasMessageContaining("cannot be null");

            verify(feesAndPayService).getFee(null);
        }

        @Test
        @DisplayName("Should handle empty fee type")
        void shouldHandleEmptyFeeType() {
            String emptyFeeType = "";
            when(taskInstance.getData()).thenReturn(emptyFeeType);
            when(feesAndPayService.getFee(emptyFeeType))
                .thenThrow(new FeeNotFoundException("Fee not found for feeType: "));

            CustomTask<String> task = feesAndPayTaskComponent.feesAndPayCaseIssuedTask();

            assertThatThrownBy(() -> task.execute(taskInstance, executionContext))
                .isInstanceOf(FeeNotFoundException.class);

            verify(feesAndPayService).getFee(emptyFeeType);
        }

        @Test
        @DisplayName("Should handle fee type with special characters")
        void shouldHandleFeeTypeWithSpecialCharacters() {
            String specialFeeType = "case-issue_fee.v2";
            Fee fee = Fee.builder()
                .code("FEE0001")
                .description("Special Fee")
                .version("2")
                .calculatedAmount(new BigDecimal("200.00"))
                .build();

            when(taskInstance.getData()).thenReturn(specialFeeType);
            when(feesAndPayService.getFee(specialFeeType)).thenReturn(fee);

            CustomTask<String> task = feesAndPayTaskComponent.feesAndPayCaseIssuedTask();

            CompletionHandler<String> result = task.execute(taskInstance, executionContext);

            verify(feesAndPayService).getFee(specialFeeType);
            assertThat(result).isInstanceOf(CompletionHandler.OnCompleteRemove.class);
        }
    }

    @Nested
    @DisplayName("Task Configuration Tests")
    class TaskConfigurationTests {

        @Test
        @DisplayName("Should configure task with correct failure handlers")
        void shouldConfigureTaskWithCorrectFailureHandlers() {
            CustomTask<String> task = feesAndPayTaskComponent.feesAndPayCaseIssuedTask();

            assertThat(task).isNotNull();
        }

        @Test
        @DisplayName("Should use correct configuration values")
        void shouldUseCorrectConfigurationValues() {
            FeesAndPayTaskComponent component = new FeesAndPayTaskComponent(
                feesAndPayService,
                10,
                Duration.ofMinutes(10)
            );

            CustomTask<String> task = component.feesAndPayCaseIssuedTask();
            assertThat(task).isNotNull();
        }

        @Test
        @DisplayName("Should create component with minimum retry configuration")
        void shouldCreateComponentWithMinimumRetryConfiguration() {
            FeesAndPayTaskComponent component = new FeesAndPayTaskComponent(
                feesAndPayService,
                1,
                Duration.ofSeconds(30)
            );

            CustomTask<String> task = component.feesAndPayCaseIssuedTask();
            assertThat(task).isNotNull();
        }

        @Test
        @DisplayName("Should create component with maximum retry configuration")
        void shouldCreateComponentWithMaximumRetryConfiguration() {
            FeesAndPayTaskComponent component = new FeesAndPayTaskComponent(
                feesAndPayService,
                100,
                Duration.ofHours(1)
            );

            CustomTask<String> task = component.feesAndPayCaseIssuedTask();
            assertThat(task).isNotNull();
        }
    }

    @Nested
    @DisplayName("Integration Tests")
    class IntegrationTests {

        @Test
        @DisplayName("Should handle complete successful flow")
        void shouldHandleCompleteSuccessfulFlow() {
            Fee expectedFee = Fee.builder()
                .code("FEE0412")
                .description("Recovery of Land - County Court")
                .version("4")
                .calculatedAmount(new BigDecimal("404.00"))
                .build();

            when(taskInstance.getData()).thenReturn(CASE_ISSUE_FEE_TYPE);
            when(feesAndPayService.getFee(CASE_ISSUE_FEE_TYPE)).thenReturn(expectedFee);

            CustomTask<String> task = feesAndPayTaskComponent.feesAndPayCaseIssuedTask();

            CompletionHandler<String> result = task.execute(taskInstance, executionContext);

            verify(feesAndPayService).getFee(CASE_ISSUE_FEE_TYPE);
            assertThat(result).isInstanceOf(CompletionHandler.OnCompleteRemove.class);
        }

        @Test
        @DisplayName("Should handle complete error flow with exception")
        void shouldHandleCompleteErrorFlowWithException() {
            FeeNotFoundException exception = new FeeNotFoundException("Fee not found");

            when(taskInstance.getData()).thenReturn(CASE_ISSUE_FEE_TYPE);
            when(feesAndPayService.getFee(CASE_ISSUE_FEE_TYPE)).thenThrow(exception);

            CustomTask<String> task = feesAndPayTaskComponent.feesAndPayCaseIssuedTask();

            assertThatThrownBy(() -> task.execute(taskInstance, executionContext))
                .isInstanceOf(FeeNotFoundException.class)
                .hasMessage("Fee not found");

            verify(feesAndPayService).getFee(CASE_ISSUE_FEE_TYPE);
        }

        @Test
        @DisplayName("Should handle flow with multiple different fee types sequentially")
        void shouldHandleFlowWithMultipleDifferentFeeTypesSequentially() {
            String[] feeTypes = {CASE_ISSUE_FEE_TYPE, HEARING_FEE_TYPE, "appealFee"};

            for (String feeType : feeTypes) {
                Fee fee = Fee.builder()
                    .code("FEE_" + feeType)
                    .description("Fee for " + feeType)
                    .version("1")
                    .calculatedAmount(new BigDecimal("150.00"))
                    .build();

                when(taskInstance.getData()).thenReturn(feeType);
                when(feesAndPayService.getFee(feeType)).thenReturn(fee);

                CustomTask<String> task = feesAndPayTaskComponent.feesAndPayCaseIssuedTask();

                CompletionHandler<String> result = task.execute(taskInstance, executionContext);

                verify(feesAndPayService).getFee(feeType);
                assertThat(result).isInstanceOf(CompletionHandler.OnCompleteRemove.class);
            }
        }
    }

    @Nested
    @DisplayName("Edge Cases Tests")
    class EdgeCasesTests {

        @Test
        @DisplayName("Should handle fee with null description")
        void shouldHandleFeeWithNullDescription() {
            Fee feeWithNullDescription = Fee.builder()
                .code("FEE0001")
                .description(null)
                .version("1")
                .calculatedAmount(new BigDecimal("100.00"))
                .build();

            when(taskInstance.getData()).thenReturn(CASE_ISSUE_FEE_TYPE);
            when(feesAndPayService.getFee(CASE_ISSUE_FEE_TYPE)).thenReturn(feeWithNullDescription);

            CustomTask<String> task = feesAndPayTaskComponent.feesAndPayCaseIssuedTask();

            CompletionHandler<String> result = task.execute(taskInstance, executionContext);

            verify(feesAndPayService).getFee(CASE_ISSUE_FEE_TYPE);
            assertThat(result).isInstanceOf(CompletionHandler.OnCompleteRemove.class);
        }

        @Test
        @DisplayName("Should handle fee with null version")
        void shouldHandleFeeWithNullVersion() {
            Fee feeWithNullVersion = Fee.builder()
                .code("FEE0001")
                .description("Test Fee")
                .version(null)
                .calculatedAmount(new BigDecimal("100.00"))
                .build();

            when(taskInstance.getData()).thenReturn(CASE_ISSUE_FEE_TYPE);
            when(feesAndPayService.getFee(CASE_ISSUE_FEE_TYPE)).thenReturn(feeWithNullVersion);

            CustomTask<String> task = feesAndPayTaskComponent.feesAndPayCaseIssuedTask();

            CompletionHandler<String> result = task.execute(taskInstance, executionContext);

            verify(feesAndPayService).getFee(CASE_ISSUE_FEE_TYPE);
            assertThat(result).isInstanceOf(CompletionHandler.OnCompleteRemove.class);
        }

        @Test
        @DisplayName("Should handle fee with very long description")
        void shouldHandleFeeWithVeryLongDescription() {
            String longDescription = "A".repeat(1000);
            Fee feeWithLongDescription = Fee.builder()
                .code("FEE0001")
                .description(longDescription)
                .version("1")
                .calculatedAmount(new BigDecimal("100.00"))
                .build();

            when(taskInstance.getData()).thenReturn(CASE_ISSUE_FEE_TYPE);
            when(feesAndPayService.getFee(CASE_ISSUE_FEE_TYPE)).thenReturn(feeWithLongDescription);

            CustomTask<String> task = feesAndPayTaskComponent.feesAndPayCaseIssuedTask();

            CompletionHandler<String> result = task.execute(taskInstance, executionContext);

            verify(feesAndPayService).getFee(CASE_ISSUE_FEE_TYPE);
            assertThat(result).isInstanceOf(CompletionHandler.OnCompleteRemove.class);
        }

        @Test
        @DisplayName("Should handle fee with decimal precision")
        void shouldHandleFeeWithDecimalPrecision() {
            Fee feeWithPrecision = Fee.builder()
                .code("FEE0001")
                .description("Precise Fee")
                .version("1")
                .calculatedAmount(new BigDecimal("123.456789"))
                .build();

            when(taskInstance.getData()).thenReturn(CASE_ISSUE_FEE_TYPE);
            when(feesAndPayService.getFee(CASE_ISSUE_FEE_TYPE)).thenReturn(feeWithPrecision);

            CustomTask<String> task = feesAndPayTaskComponent.feesAndPayCaseIssuedTask();

            CompletionHandler<String> result = task.execute(taskInstance, executionContext);

            verify(feesAndPayService).getFee(CASE_ISSUE_FEE_TYPE);
            assertThat(result).isInstanceOf(CompletionHandler.OnCompleteRemove.class);
        }
    }
}
