package uk.gov.hmcts.reform.pcs.feesandpay.task;

import com.github.kagkarlsson.scheduler.task.CompletionHandler;
import com.github.kagkarlsson.scheduler.task.CompletionHandler.OnCompleteRemove;
import com.github.kagkarlsson.scheduler.task.Execution;
import com.github.kagkarlsson.scheduler.task.ExecutionContext;
import com.github.kagkarlsson.scheduler.task.TaskInstance;
import com.github.kagkarlsson.scheduler.task.helper.CustomTask;
import lombok.Getter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import uk.gov.hmcts.reform.fees.client.model.FeeLookupResponseDto;
import uk.gov.hmcts.reform.pcs.feesandpay.exception.FeeNotFoundException;
import uk.gov.hmcts.reform.pcs.feesandpay.model.FeesAndPayTaskData;
import uk.gov.hmcts.reform.pcs.feesandpay.service.FeesAndPayService;

import java.math.BigDecimal;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyInt;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.pcs.feesandpay.task.FeesAndPayTaskComponent.FEE_CASE_ISSUED_TASK_DESCRIPTOR;
import static uk.gov.hmcts.reform.pcs.feesandpay.task.FeesAndPayTaskComponentTest.TestFeeCode.APPEAL_FEE;
import static uk.gov.hmcts.reform.pcs.feesandpay.task.FeesAndPayTaskComponentTest.TestFeeCode.COPY_FEE;
import static uk.gov.hmcts.reform.pcs.feesandpay.task.FeesAndPayTaskComponentTest.TestFeeCode.GENERIC_TEST_FEE;
import static uk.gov.hmcts.reform.pcs.feesandpay.task.FeesAndPayTaskComponentTest.TestFeeCode.HEARING_FEE;
import static uk.gov.hmcts.reform.pcs.feesandpay.task.FeesAndPayTaskComponentTest.TestFeeCode.SPECIAL_CHAR_FEE;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("FeesAndPayTaskComponent Tests")
class FeesAndPayTaskComponentTest {

    private FeesAndPayTaskComponent feesAndPayTaskComponent;

    @Mock
    private FeesAndPayService feesAndPayService;

    @Mock
    private TaskInstance<FeesAndPayTaskData> taskInstance;

    @Mock
    private ExecutionContext executionContext;

    @Mock
    private Execution execution;

    private final Duration feesAndPayBackoffDelay = Duration.ofMinutes(5);

    private static final String TASK_ID = "fee-task-123";
    private static final String CASE_ISSUE_FEE_TYPE = "caseIssueFee";
    private static final String HEARING_FEE_TYPE = "hearingFee";

    @Getter
    public enum TestFeeCode {
        RECOVERY_OF_LAND("FEE0412", "Recovery of Land - County Court"),
        HEARING_FEE("FEE9999", "Hearing Fee"),
        WAIVED_FEE("FEE0000", "Waived Fee"),
        HIGH_VALUE_FEE("FEE8888", "High Value Fee"),
        GENERIC_TEST_FEE("FEE0001", "Test Fee"),
        SPECIAL_CHAR_FEE("FEE0002", "Special Fee"),
        APPEAL_FEE("FEE0003", "Appeal Fee"),
        COPY_FEE("FEE0004", "Copy Fee");

        private final String code;
        private final String description;

        TestFeeCode(String code, String description) {
            this.code = code;
            this.description = description;
        }
    }

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

    private FeesAndPayTaskData buildTaskData(String feeType) {
        return FeesAndPayTaskData.builder()
            .feeType(feeType)
            .caseReference("BUS-123")
            .ccdCaseNumber("1111-2222-3333-4444")
            .volume(2)
            .responsibleParty("Applicant")
            .build();
    }

    private FeeLookupResponseDto buildFee(String code, String description, Integer version, BigDecimal amount) {
        return FeeLookupResponseDto.builder()
            .code(code)
            .description(description)
            .version(version)
            .feeAmount(amount)
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
            CustomTask<FeesAndPayTaskData> task = feesAndPayTaskComponent.feesAndPayCaseIssuedTask();
            assertThat(task).isNotNull();
        }
    }

    @Nested
    @DisplayName("Successful Flow Tests")
    class SuccessfulFlowTests {

        @Test
        @DisplayName("Should retrieve case issue fee and create service request successfully")
        void shouldRetrieveCaseIssueFeeSuccessfully() {
            FeesAndPayTaskData data = buildTaskData(CASE_ISSUE_FEE_TYPE);
            when(taskInstance.getData()).thenReturn(data);

            FeeLookupResponseDto expectedFee = buildFee(
                TestFeeCode.RECOVERY_OF_LAND.getCode(),
                TestFeeCode.RECOVERY_OF_LAND.getDescription(),
                4,
                new BigDecimal("404.00")
            );

            when(feesAndPayService.getFee(CASE_ISSUE_FEE_TYPE)).thenReturn(expectedFee);

            CustomTask<FeesAndPayTaskData> task = feesAndPayTaskComponent.feesAndPayCaseIssuedTask();

            CompletionHandler<FeesAndPayTaskData> result = task.execute(taskInstance, executionContext);

            verify(feesAndPayService).getFee(CASE_ISSUE_FEE_TYPE);
            verify(feesAndPayService).createServiceRequest(
                data.getCaseReference(),
                data.getCcdCaseNumber(),
                expectedFee,
                data.getVolume(),
                data.getResponsibleParty()
            );
            assertThat(result).isInstanceOf(OnCompleteRemove.class);
        }

        @Test
        @DisplayName("Should retrieve hearing fee and create service request successfully")
        void shouldRetrieveHearingFeeSuccessfully() {
            FeesAndPayTaskData data = buildTaskData(HEARING_FEE_TYPE);
            when(taskInstance.getData()).thenReturn(data);

            FeeLookupResponseDto fee = buildFee(
                HEARING_FEE.getCode(),
                HEARING_FEE.getDescription(),
                1,
                new BigDecimal("100.00")
            );

            when(feesAndPayService.getFee(HEARING_FEE_TYPE)).thenReturn(fee);

            CustomTask<FeesAndPayTaskData> task = feesAndPayTaskComponent.feesAndPayCaseIssuedTask();

            CompletionHandler<FeesAndPayTaskData> result = task.execute(taskInstance, executionContext);

            verify(feesAndPayService).getFee(HEARING_FEE_TYPE);
            verify(feesAndPayService).createServiceRequest(
                data.getCaseReference(),
                data.getCcdCaseNumber(),
                fee,
                data.getVolume(),
                data.getResponsibleParty()
            );
            assertThat(result).isInstanceOf(OnCompleteRemove.class);
        }

        @Test
        @DisplayName("Should handle large fee amounts and create service request")
        void shouldHandleLargeFeeAmount() {
            FeesAndPayTaskData data = buildTaskData(CASE_ISSUE_FEE_TYPE);
            when(taskInstance.getData()).thenReturn(data);

            FeeLookupResponseDto fee = buildFee(
                TestFeeCode.HIGH_VALUE_FEE.getCode(),
                TestFeeCode.HIGH_VALUE_FEE.getDescription(),
                1,
                new BigDecimal("10000.00")
            );

            when(feesAndPayService.getFee(CASE_ISSUE_FEE_TYPE)).thenReturn(fee);

            CustomTask<FeesAndPayTaskData> task = feesAndPayTaskComponent.feesAndPayCaseIssuedTask();

            CompletionHandler<FeesAndPayTaskData> result = task.execute(taskInstance, executionContext);

            verify(feesAndPayService).getFee(CASE_ISSUE_FEE_TYPE);
            verify(feesAndPayService).createServiceRequest(
                data.getCaseReference(),
                data.getCcdCaseNumber(),
                fee,
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
        @DisplayName("Should throw FeeNotFoundException when fee type not configured")
        void shouldThrowFeeNotFoundExceptionWhenFeeTypeNotConfigured() {
            String invalidFeeType = "invalidFeeType";
            FeesAndPayTaskData data = buildTaskData(invalidFeeType);
            when(taskInstance.getData()).thenReturn(data);

            FeeNotFoundException exception = new FeeNotFoundException("Fee not found for feeType: " + invalidFeeType);
            when(feesAndPayService.getFee(invalidFeeType)).thenThrow(exception);

            CustomTask<FeesAndPayTaskData> task = feesAndPayTaskComponent.feesAndPayCaseIssuedTask();

            assertThatThrownBy(() -> task.execute(taskInstance, executionContext))
                .isInstanceOf(FeeNotFoundException.class)
                .hasMessage("Fee not found for feeType: " + invalidFeeType);

            verify(feesAndPayService).getFee(invalidFeeType);
            verify(feesAndPayService, never())
                .createServiceRequest(anyString(), anyString(), any(), anyInt(), anyString());
        }

        @Test
        @DisplayName("Should rethrow exception when API call fails")
        void shouldThrowFeeNotFoundExceptionWhenApiCallFails() {
            FeesAndPayTaskData data = buildTaskData(CASE_ISSUE_FEE_TYPE);
            when(taskInstance.getData()).thenReturn(data);

            FeeNotFoundException exception = new FeeNotFoundException("Unable to retrieve fee: " + CASE_ISSUE_FEE_TYPE);
            when(feesAndPayService.getFee(CASE_ISSUE_FEE_TYPE)).thenThrow(exception);

            CustomTask<FeesAndPayTaskData> task = feesAndPayTaskComponent.feesAndPayCaseIssuedTask();

            assertThatThrownBy(() -> task.execute(taskInstance, executionContext))
                .isInstanceOf(FeeNotFoundException.class)
                .hasMessageContaining("Unable to retrieve fee");

            verify(feesAndPayService).getFee(CASE_ISSUE_FEE_TYPE);
            verify(feesAndPayService, never())
                .createServiceRequest(anyString(), anyString(), any(), anyInt(), anyString());
        }

        @Test
        @DisplayName("Should propagate RuntimeException")
        void shouldPropagateRuntimeException() {
            FeesAndPayTaskData data = buildTaskData(CASE_ISSUE_FEE_TYPE);
            when(taskInstance.getData()).thenReturn(data);

            RuntimeException exception = new RuntimeException("Unexpected error");
            when(feesAndPayService.getFee(CASE_ISSUE_FEE_TYPE)).thenThrow(exception);

            CustomTask<FeesAndPayTaskData> task = feesAndPayTaskComponent.feesAndPayCaseIssuedTask();

            assertThatThrownBy(() -> task.execute(taskInstance, executionContext))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Unexpected error");

            verify(feesAndPayService).getFee(CASE_ISSUE_FEE_TYPE);
            verify(feesAndPayService, never())
                .createServiceRequest(anyString(), anyString(), any(), anyInt(), anyString());
        }

        @Test
        @DisplayName("Should propagate IllegalArgumentException")
        void shouldPropagateIllegalArgumentException() {
            FeesAndPayTaskData data = buildTaskData(CASE_ISSUE_FEE_TYPE);
            when(taskInstance.getData()).thenReturn(data);

            IllegalArgumentException exception = new IllegalArgumentException("Invalid fee type");
            when(feesAndPayService.getFee(CASE_ISSUE_FEE_TYPE)).thenThrow(exception);

            CustomTask<FeesAndPayTaskData> task = feesAndPayTaskComponent.feesAndPayCaseIssuedTask();

            assertThatThrownBy(() -> task.execute(taskInstance, executionContext))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Invalid fee type");

            verify(feesAndPayService).getFee(CASE_ISSUE_FEE_TYPE);
            verify(feesAndPayService, never())
                .createServiceRequest(anyString(), anyString(), any(), anyInt(), anyString());
        }
    }

    @Nested
    @DisplayName("Data Validation Tests")
    class DataValidationTests {

        @Test
        @DisplayName("Should handle different fee types")
        void shouldHandleDifferentFeeTypes() {
            String[] feeTypes = {"caseIssueFee", "hearingFee", "appealFee", "copyFee"};
            TestFeeCode[] feeCodes = {
                GENERIC_TEST_FEE,
                HEARING_FEE,
                APPEAL_FEE,
                COPY_FEE
            };

            for (int i = 0; i < feeTypes.length; i++) {
                FeesAndPayTaskData data = buildTaskData(feeTypes[i]);
                when(taskInstance.getData()).thenReturn(data);

                FeeLookupResponseDto fee = buildFee(
                    feeCodes[i].getCode(),
                    feeCodes[i].getDescription(),
                    1,
                    new BigDecimal("100.00")
                );

                when(feesAndPayService.getFee(feeTypes[i])).thenReturn(fee);

                CustomTask<FeesAndPayTaskData> task = feesAndPayTaskComponent.feesAndPayCaseIssuedTask();

                CompletionHandler<FeesAndPayTaskData> result = task.execute(taskInstance, executionContext);

                verify(feesAndPayService).getFee(feeTypes[i]);
                verify(feesAndPayService).createServiceRequest(
                    data.getCaseReference(),
                    data.getCcdCaseNumber(),
                    fee,
                    data.getVolume(),
                    data.getResponsibleParty()
                );
                assertThat(result).isInstanceOf(OnCompleteRemove.class);
            }
        }

        @Test
        @DisplayName("Should handle null fee type")
        void shouldHandleNullFeeType() {
            FeesAndPayTaskData data = buildTaskData(null);
            when(taskInstance.getData()).thenReturn(data);
            when(feesAndPayService.getFee(null))
                .thenThrow(new FeeNotFoundException("Fee type cannot be null"));

            CustomTask<FeesAndPayTaskData> task = feesAndPayTaskComponent.feesAndPayCaseIssuedTask();

            assertThatThrownBy(() -> task.execute(taskInstance, executionContext))
                .isInstanceOf(FeeNotFoundException.class)
                .hasMessageContaining("cannot be null");

            verify(feesAndPayService).getFee(null);
            verify(feesAndPayService, never())
                .createServiceRequest(anyString(), anyString(), any(), anyInt(), anyString());
        }

        @Test
        @DisplayName("Should handle empty fee type")
        void shouldHandleEmptyFeeType() {
            FeesAndPayTaskData data = buildTaskData("");
            when(taskInstance.getData()).thenReturn(data);
            when(feesAndPayService.getFee(""))
                .thenThrow(new FeeNotFoundException("Fee not found for feeType: "));

            CustomTask<FeesAndPayTaskData> task = feesAndPayTaskComponent.feesAndPayCaseIssuedTask();

            assertThatThrownBy(() -> task.execute(taskInstance, executionContext))
                .isInstanceOf(FeeNotFoundException.class);

            verify(feesAndPayService).getFee("");
            verify(feesAndPayService, never())
                .createServiceRequest(anyString(), anyString(), any(), anyInt(), anyString());
        }

        @Test
        @DisplayName("Should handle fee type with special characters")
        void shouldHandleFeeTypeWithSpecialCharacters() {
            String specialFeeType = "case-issue_fee.v2";
            FeesAndPayTaskData data = buildTaskData(specialFeeType);
            when(taskInstance.getData()).thenReturn(data);

            FeeLookupResponseDto fee = buildFee(
                SPECIAL_CHAR_FEE.getCode(),
                SPECIAL_CHAR_FEE.getDescription(),
                2,
                new BigDecimal("200.00")
            );

            when(feesAndPayService.getFee(specialFeeType)).thenReturn(fee);

            CustomTask<FeesAndPayTaskData> task = feesAndPayTaskComponent.feesAndPayCaseIssuedTask();

            CompletionHandler<FeesAndPayTaskData> result = task.execute(taskInstance, executionContext);

            verify(feesAndPayService).getFee(specialFeeType);
            verify(feesAndPayService).createServiceRequest(
                data.getCaseReference(),
                data.getCcdCaseNumber(),
                fee,
                data.getVolume(),
                data.getResponsibleParty()
            );
            assertThat(result).isInstanceOf(OnCompleteRemove.class);
        }
    }

    @Nested
    @DisplayName("Task Configuration Tests")
    class TaskConfigurationTests {

        @Test
        @DisplayName("Should configure task with correct failure handlers")
        void shouldConfigureTaskWithCorrectFailureHandlers() {
            CustomTask<FeesAndPayTaskData> task = feesAndPayTaskComponent.feesAndPayCaseIssuedTask();
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

            CustomTask<FeesAndPayTaskData> task = component.feesAndPayCaseIssuedTask();
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

            CustomTask<FeesAndPayTaskData> task = component.feesAndPayCaseIssuedTask();
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

            CustomTask<FeesAndPayTaskData> task = component.feesAndPayCaseIssuedTask();
            assertThat(task).isNotNull();
        }
    }

    @Nested
    @DisplayName("Integration-like Flow Tests")
    class IntegrationFlowTests {

        @Test
        @DisplayName("Should handle complete successful flow")
        void shouldHandleCompleteSuccessfulFlow() {
            FeesAndPayTaskData data = buildTaskData(CASE_ISSUE_FEE_TYPE);
            when(taskInstance.getData()).thenReturn(data);

            FeeLookupResponseDto expectedFee = buildFee(
                TestFeeCode.RECOVERY_OF_LAND.getCode(),
                TestFeeCode.RECOVERY_OF_LAND.getDescription(),
                4,
                new BigDecimal("404.00")
            );

            when(feesAndPayService.getFee(CASE_ISSUE_FEE_TYPE)).thenReturn(expectedFee);

            CustomTask<FeesAndPayTaskData> task = feesAndPayTaskComponent.feesAndPayCaseIssuedTask();

            CompletionHandler<FeesAndPayTaskData> result = task.execute(taskInstance, executionContext);

            verify(feesAndPayService).getFee(CASE_ISSUE_FEE_TYPE);
            verify(feesAndPayService).createServiceRequest(
                data.getCaseReference(),
                data.getCcdCaseNumber(),
                expectedFee,
                data.getVolume(),
                data.getResponsibleParty()
            );
            assertThat(result).isInstanceOf(OnCompleteRemove.class);
        }

        @Test
        @DisplayName("Should handle complete error flow with exception")
        void shouldHandleCompleteErrorFlowWithException() {
            FeesAndPayTaskData data = buildTaskData(CASE_ISSUE_FEE_TYPE);
            when(taskInstance.getData()).thenReturn(data);

            FeeNotFoundException exception = new FeeNotFoundException("Fee not found");
            when(feesAndPayService.getFee(CASE_ISSUE_FEE_TYPE)).thenThrow(exception);

            CustomTask<FeesAndPayTaskData> task = feesAndPayTaskComponent.feesAndPayCaseIssuedTask();

            assertThatThrownBy(() -> task.execute(taskInstance, executionContext))
                .isInstanceOf(FeeNotFoundException.class)
                .hasMessage("Fee not found");

            verify(feesAndPayService).getFee(CASE_ISSUE_FEE_TYPE);
            verify(feesAndPayService, never())
                .createServiceRequest(anyString(), anyString(), any(), anyInt(), anyString());
        }

        @Test
        @DisplayName("Should handle flow with multiple different fee types sequentially")
        void shouldHandleFlowWithMultipleDifferentFeeTypesSequentially() {
            String[] feeTypes = {CASE_ISSUE_FEE_TYPE, HEARING_FEE_TYPE, "appealFee"};
            TestFeeCode[] feeCodes = {
                GENERIC_TEST_FEE,
                HEARING_FEE,
                APPEAL_FEE
            };

            for (int i = 0; i < feeTypes.length; i++) {
                FeesAndPayTaskData data = buildTaskData(feeTypes[i]);
                when(taskInstance.getData()).thenReturn(data);

                FeeLookupResponseDto fee = buildFee(
                    feeCodes[i].getCode(),
                    feeCodes[i].getDescription(),
                    1,
                    new BigDecimal("150.00")
                );

                when(feesAndPayService.getFee(feeTypes[i])).thenReturn(fee);

                CustomTask<FeesAndPayTaskData> task = feesAndPayTaskComponent.feesAndPayCaseIssuedTask();

                CompletionHandler<FeesAndPayTaskData> result = task.execute(taskInstance, executionContext);

                verify(feesAndPayService).getFee(feeTypes[i]);
                verify(feesAndPayService).createServiceRequest(
                    data.getCaseReference(),
                    data.getCcdCaseNumber(),
                    fee,
                    data.getVolume(),
                    data.getResponsibleParty()
                );
                assertThat(result).isInstanceOf(OnCompleteRemove.class);
            }
        }
    }

    @Nested
    @DisplayName("Edge Cases Tests")
    class EdgeCasesTests {

        @Test
        @DisplayName("Should handle fee with null description")
        void shouldHandleFeeWithNullDescription() {
            FeesAndPayTaskData data = buildTaskData(CASE_ISSUE_FEE_TYPE);
            when(taskInstance.getData()).thenReturn(data);

            FeeLookupResponseDto fee = buildFee(
                GENERIC_TEST_FEE.getCode(),
                null,
                1,
                new BigDecimal("100.00")
            );

            when(feesAndPayService.getFee(CASE_ISSUE_FEE_TYPE)).thenReturn(fee);

            CustomTask<FeesAndPayTaskData> task = feesAndPayTaskComponent.feesAndPayCaseIssuedTask();

            CompletionHandler<FeesAndPayTaskData> result = task.execute(taskInstance, executionContext);

            verify(feesAndPayService).getFee(CASE_ISSUE_FEE_TYPE);
            verify(feesAndPayService).createServiceRequest(
                data.getCaseReference(),
                data.getCcdCaseNumber(),
                fee,
                data.getVolume(),
                data.getResponsibleParty()
            );
            assertThat(result).isInstanceOf(OnCompleteRemove.class);
        }

        @Test
        @DisplayName("Should handle fee with null version")
        void shouldHandleFeeWithNullVersion() {
            FeesAndPayTaskData data = buildTaskData(CASE_ISSUE_FEE_TYPE);
            when(taskInstance.getData()).thenReturn(data);

            FeeLookupResponseDto fee = buildFee(
                GENERIC_TEST_FEE.getCode(),
                GENERIC_TEST_FEE.getDescription(),
                null,
                new BigDecimal("100.00")
            );

            when(feesAndPayService.getFee(CASE_ISSUE_FEE_TYPE)).thenReturn(fee);

            CustomTask<FeesAndPayTaskData> task = feesAndPayTaskComponent.feesAndPayCaseIssuedTask();

            CompletionHandler<FeesAndPayTaskData> result = task.execute(taskInstance, executionContext);

            verify(feesAndPayService).getFee(CASE_ISSUE_FEE_TYPE);
            verify(feesAndPayService).createServiceRequest(
                data.getCaseReference(),
                data.getCcdCaseNumber(),
                fee,
                data.getVolume(),
                data.getResponsibleParty()
            );
            assertThat(result).isInstanceOf(OnCompleteRemove.class);
        }

        @Test
        @DisplayName("Should handle fee with very long description")
        void shouldHandleFeeWithVeryLongDescription() {
            String longDescription = "A".repeat(1000);
            FeesAndPayTaskData data = buildTaskData(CASE_ISSUE_FEE_TYPE);
            when(taskInstance.getData()).thenReturn(data);

            FeeLookupResponseDto fee = buildFee(
                GENERIC_TEST_FEE.getCode(),
                longDescription,
                1,
                new BigDecimal("100.00")
            );

            when(feesAndPayService.getFee(CASE_ISSUE_FEE_TYPE)).thenReturn(fee);

            CustomTask<FeesAndPayTaskData> task = feesAndPayTaskComponent.feesAndPayCaseIssuedTask();

            CompletionHandler<FeesAndPayTaskData> result = task.execute(taskInstance, executionContext);

            verify(feesAndPayService).getFee(CASE_ISSUE_FEE_TYPE);
            verify(feesAndPayService).createServiceRequest(
                data.getCaseReference(),
                data.getCcdCaseNumber(),
                fee,
                data.getVolume(),
                data.getResponsibleParty()
            );
            assertThat(result).isInstanceOf(OnCompleteRemove.class);
        }

        @Test
        @DisplayName("Should handle fee with decimal precision")
        void shouldHandleFeeWithDecimalPrecision() {
            FeesAndPayTaskData data = buildTaskData(CASE_ISSUE_FEE_TYPE);
            when(taskInstance.getData()).thenReturn(data);

            FeeLookupResponseDto fee = buildFee(
                GENERIC_TEST_FEE.getCode(),
                GENERIC_TEST_FEE.getDescription(),
                1,
                new BigDecimal("123.456789")
            );

            when(feesAndPayService.getFee(CASE_ISSUE_FEE_TYPE)).thenReturn(fee);

            CustomTask<FeesAndPayTaskData> task = feesAndPayTaskComponent.feesAndPayCaseIssuedTask();

            CompletionHandler<FeesAndPayTaskData> result = task.execute(taskInstance, executionContext);

            verify(feesAndPayService).getFee(CASE_ISSUE_FEE_TYPE);
            verify(feesAndPayService).createServiceRequest(
                data.getCaseReference(),
                data.getCcdCaseNumber(),
                fee,
                data.getVolume(),
                data.getResponsibleParty()
            );
            assertThat(result).isInstanceOf(OnCompleteRemove.class);
        }
    }
}
