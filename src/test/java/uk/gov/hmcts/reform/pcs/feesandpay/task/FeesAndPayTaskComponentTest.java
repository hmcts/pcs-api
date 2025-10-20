package uk.gov.hmcts.reform.pcs.feesandpay.task;

import com.github.kagkarlsson.scheduler.task.CompletionHandler;
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
import uk.gov.hmcts.reform.payments.response.PaymentServiceResponse;
import uk.gov.hmcts.reform.pcs.feesandpay.entity.Fee;
import uk.gov.hmcts.reform.pcs.feesandpay.exception.FeeNotFoundException;
import uk.gov.hmcts.reform.pcs.feesandpay.model.FeesAndPayTaskData;
import uk.gov.hmcts.reform.pcs.feesandpay.service.FeesAndPayService;

import java.math.BigDecimal;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.quality.Strictness.LENIENT;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = LENIENT)
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

    @Mock
    private PaymentServiceResponse paymentServiceResponse;

    private final Duration feesAndPayBackoffDelay = Duration.ofMinutes(5);

    private static final String TASK_ID = "fee-task-123";
    private static final String CASE_ISSUE_FEE_TYPE = "caseIssueFee";
    private static final String HEARING_FEE_TYPE = "hearingFee";
    private static final String CASE_REFERENCE = "123456";
    private static final String CCD_CASE_NUMBER = "CCD123";
    private static final String RESPONSIBLE_PARTY = "Claimant";

    /**
     * Test-specific enum for fee codes used in unit tests.
     */
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

    private FeesAndPayTaskData createTaskData(String feeType) {
        return FeesAndPayTaskData.builder()
            .feeType(feeType)
            .caseReference(CASE_REFERENCE)
            .ccdCaseNumber(CCD_CASE_NUMBER)
            .volume(1)
            .responsibleParty(RESPONSIBLE_PARTY)
            .build();
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
    @DisplayName("Successful Fee Retrieval and Service Request Tests")
    class SuccessfulFeeRetrievalTests {

        @Test
        @DisplayName("Should retrieve case issue fee and create service request successfully")
        void shouldRetrieveCaseIssueFeeAndCreateServiceRequestSuccessfully() {
            FeesAndPayTaskData taskData = createTaskData(CASE_ISSUE_FEE_TYPE);
            Fee expectedFee = Fee.builder()
                .code(TestFeeCode.RECOVERY_OF_LAND.getCode())
                .description(TestFeeCode.RECOVERY_OF_LAND.getDescription())
                .version("4")
                .calculatedAmount(new BigDecimal("404.00"))
                .build();

            when(taskInstance.getData()).thenReturn(taskData);
            when(feesAndPayService.getFee(CASE_ISSUE_FEE_TYPE)).thenReturn(expectedFee);
            when(feesAndPayService.createServiceRequest(
                CASE_REFERENCE, CCD_CASE_NUMBER, expectedFee, 1, RESPONSIBLE_PARTY))
                .thenReturn(paymentServiceResponse);

            CustomTask<FeesAndPayTaskData> task = feesAndPayTaskComponent.feesAndPayCaseIssuedTask();

            CompletionHandler<FeesAndPayTaskData> result = task.execute(taskInstance, executionContext);

            verify(feesAndPayService).getFee(CASE_ISSUE_FEE_TYPE);
            verify(feesAndPayService).createServiceRequest(
                CASE_REFERENCE,
                CCD_CASE_NUMBER,
                expectedFee,
                1,
                RESPONSIBLE_PARTY
            );
            assertThat(result).isInstanceOf(CompletionHandler.OnCompleteRemove.class);
        }

        @Test
        @DisplayName("Should retrieve hearing fee and create service request successfully")
        void shouldRetrieveHearingFeeSuccessfully() {
            FeesAndPayTaskData taskData = createTaskData(HEARING_FEE_TYPE);
            Fee expectedFee = Fee.builder()
                .code(TestFeeCode.HEARING_FEE.getCode())
                .description(TestFeeCode.HEARING_FEE.getDescription())
                .version("1")
                .calculatedAmount(new BigDecimal("100.00"))
                .build();

            when(taskInstance.getData()).thenReturn(taskData);
            when(feesAndPayService.getFee(HEARING_FEE_TYPE)).thenReturn(expectedFee);
            when(feesAndPayService.createServiceRequest(any(), any(), any(), any(Integer.class), any()))
                .thenReturn(paymentServiceResponse);

            CustomTask<FeesAndPayTaskData> task = feesAndPayTaskComponent.feesAndPayCaseIssuedTask();

            CompletionHandler<FeesAndPayTaskData> result = task.execute(taskInstance, executionContext);

            verify(feesAndPayService).getFee(HEARING_FEE_TYPE);
            verify(feesAndPayService).createServiceRequest(
                CASE_REFERENCE,
                CCD_CASE_NUMBER,
                expectedFee,
                1,
                RESPONSIBLE_PARTY
            );
            assertThat(result).isInstanceOf(CompletionHandler.OnCompleteRemove.class);
        }

        @Test
        @DisplayName("Should handle fee with zero amount")
        void shouldHandleFeeWithZeroAmount() {
            FeesAndPayTaskData taskData = createTaskData(CASE_ISSUE_FEE_TYPE);
            Fee zeroFee = Fee.builder()
                .code(TestFeeCode.WAIVED_FEE.getCode())
                .description(TestFeeCode.WAIVED_FEE.getDescription())
                .version("1")
                .calculatedAmount(BigDecimal.ZERO)
                .build();

            when(taskInstance.getData()).thenReturn(taskData);
            when(feesAndPayService.getFee(CASE_ISSUE_FEE_TYPE)).thenReturn(zeroFee);
            when(feesAndPayService.createServiceRequest(any(), any(), any(), any(Integer.class), any()))
                .thenReturn(paymentServiceResponse);

            CustomTask<FeesAndPayTaskData> task = feesAndPayTaskComponent.feesAndPayCaseIssuedTask();

            CompletionHandler<FeesAndPayTaskData> result = task.execute(taskInstance, executionContext);

            verify(feesAndPayService).getFee(CASE_ISSUE_FEE_TYPE);
            verify(feesAndPayService).createServiceRequest(any(), any(), eq(zeroFee), any(Integer.class), any());
            assertThat(result).isInstanceOf(CompletionHandler.OnCompleteRemove.class);
        }

        @Test
        @DisplayName("Should handle different volumes in task data")
        void shouldHandleDifferentVolumesInTaskData() {
            FeesAndPayTaskData taskDataWithVolume = FeesAndPayTaskData.builder()
                .feeType(CASE_ISSUE_FEE_TYPE)
                .caseReference(CASE_REFERENCE)
                .ccdCaseNumber(CCD_CASE_NUMBER)
                .volume(5)
                .responsibleParty(RESPONSIBLE_PARTY)
                .build();

            Fee expectedFee = Fee.builder()
                .code(TestFeeCode.GENERIC_TEST_FEE.getCode())
                .description(TestFeeCode.GENERIC_TEST_FEE.getDescription())
                .version("1")
                .calculatedAmount(new BigDecimal("100.00"))
                .build();

            when(taskInstance.getData()).thenReturn(taskDataWithVolume);
            when(feesAndPayService.getFee(CASE_ISSUE_FEE_TYPE)).thenReturn(expectedFee);
            when(feesAndPayService.createServiceRequest(any(), any(), any(), eq(5), any()))
                .thenReturn(paymentServiceResponse);

            CustomTask<FeesAndPayTaskData> task = feesAndPayTaskComponent.feesAndPayCaseIssuedTask();

            CompletionHandler<FeesAndPayTaskData> result = task.execute(taskInstance, executionContext);

            verify(feesAndPayService).createServiceRequest(
                CASE_REFERENCE,
                CCD_CASE_NUMBER,
                expectedFee,
                5,
                RESPONSIBLE_PARTY
            );
            assertThat(result).isInstanceOf(CompletionHandler.OnCompleteRemove.class);
        }
    }

    @Nested
    @DisplayName("Exception Handling Tests")
    class ExceptionHandlingTests {

        @Test
        @DisplayName("Should throw FeeNotFoundException when fee type not configured")
        void shouldThrowFeeNotFoundExceptionWhenFeeTypeNotConfigured() {
            String invalidFeeType = "invalidFeeType";
            FeesAndPayTaskData taskData = createTaskData(invalidFeeType);
            FeeNotFoundException exception = new FeeNotFoundException("Fee not found for feeType: " + invalidFeeType);

            when(taskInstance.getData()).thenReturn(taskData);
            when(feesAndPayService.getFee(invalidFeeType)).thenThrow(exception);

            CustomTask<FeesAndPayTaskData> task = feesAndPayTaskComponent.feesAndPayCaseIssuedTask();

            assertThatThrownBy(() -> task.execute(taskInstance, executionContext))
                .isInstanceOf(FeeNotFoundException.class)
                .hasMessage("Fee not found for feeType: " + invalidFeeType);

            verify(feesAndPayService).getFee(invalidFeeType);
        }

        @Test
        @DisplayName("Should throw exception when service request creation fails")
        void shouldThrowExceptionWhenServiceRequestCreationFails() {
            FeesAndPayTaskData taskData = createTaskData(CASE_ISSUE_FEE_TYPE);
            Fee expectedFee = Fee.builder()
                .code(TestFeeCode.GENERIC_TEST_FEE.getCode())
                .description(TestFeeCode.GENERIC_TEST_FEE.getDescription())
                .version("1")
                .calculatedAmount(new BigDecimal("100.00"))
                .build();
            RuntimeException serviceException = new RuntimeException("Service request failed");

            when(taskInstance.getData()).thenReturn(taskData);
            when(feesAndPayService.getFee(CASE_ISSUE_FEE_TYPE)).thenReturn(expectedFee);
            when(feesAndPayService.createServiceRequest(any(), any(), any(), any(Integer.class), any()))
                .thenThrow(serviceException);

            CustomTask<FeesAndPayTaskData> task = feesAndPayTaskComponent.feesAndPayCaseIssuedTask();

            assertThatThrownBy(() -> task.execute(taskInstance, executionContext))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Service request failed");

            verify(feesAndPayService).getFee(CASE_ISSUE_FEE_TYPE);
            verify(feesAndPayService).createServiceRequest(any(), any(), any(), any(Integer.class), any());
        }
    }

    @Nested
    @DisplayName("Task Data Validation Tests")
    class TaskDataValidationTests {

        @Test
        @DisplayName("Should handle task data with different responsible parties")
        void shouldHandleTaskDataWithDifferentResponsibleParties() {
            String[] responsibleParties = {"Claimant", "Defendant", "Solicitor"};

            for (String party : responsibleParties) {
                FeesAndPayTaskData taskData = FeesAndPayTaskData.builder()
                    .feeType(CASE_ISSUE_FEE_TYPE)
                    .caseReference(CASE_REFERENCE)
                    .ccdCaseNumber(CCD_CASE_NUMBER)
                    .volume(1)
                    .responsibleParty(party)
                    .build();

                Fee fee = Fee.builder()
                    .code(TestFeeCode.GENERIC_TEST_FEE.getCode())
                    .description(TestFeeCode.GENERIC_TEST_FEE.getDescription())
                    .version("1")
                    .calculatedAmount(new BigDecimal("100.00"))
                    .build();

                when(taskInstance.getData()).thenReturn(taskData);
                when(feesAndPayService.getFee(CASE_ISSUE_FEE_TYPE)).thenReturn(fee);
                when(feesAndPayService.createServiceRequest(any(), any(), any(), any(Integer.class), eq(party)))
                    .thenReturn(paymentServiceResponse);

                CustomTask<FeesAndPayTaskData> task = feesAndPayTaskComponent.feesAndPayCaseIssuedTask();

                CompletionHandler<FeesAndPayTaskData> result = task.execute(taskInstance, executionContext);

                verify(feesAndPayService).createServiceRequest(
                    CASE_REFERENCE,
                    CCD_CASE_NUMBER,
                    fee,
                    1,
                    party
                );
                assertThat(result).isInstanceOf(CompletionHandler.OnCompleteRemove.class);
            }
        }
    }
}
