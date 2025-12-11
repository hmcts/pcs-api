package uk.gov.hmcts.reform.pcs.feesandpay.task;

import com.github.kagkarlsson.scheduler.task.CompletionHandler;
import com.github.kagkarlsson.scheduler.task.FailureHandler;
import com.github.kagkarlsson.scheduler.task.TaskDescriptor;
import com.github.kagkarlsson.scheduler.task.helper.CustomTask;
import com.github.kagkarlsson.scheduler.task.helper.Tasks;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.pcs.feesandpay.model.FeeDetails;
import uk.gov.hmcts.reform.pcs.feesandpay.model.FeesAndPayTaskData;
import uk.gov.hmcts.reform.pcs.feesandpay.service.FeeService;
import uk.gov.hmcts.reform.pcs.feesandpay.service.PaymentService;

import java.time.Duration;

@Slf4j
@Component
public class FeesAndPayTaskComponent {

    private static final String FEES_AND_PAY_CASE_ISSUED_TASK_NAME = "fees-and-pay-task";

    public static final TaskDescriptor<FeesAndPayTaskData> FEE_CASE_ISSUED_TASK_DESCRIPTOR =
        TaskDescriptor.of(FEES_AND_PAY_CASE_ISSUED_TASK_NAME, FeesAndPayTaskData.class);

    private final FeeService feeService;
    private final PaymentService paymentService;
    private final int maxRetriesFeesAndPay;
    private final Duration feesAndPayBackoffDelay;

    public FeesAndPayTaskComponent(
        FeeService feeService,
        PaymentService paymentService,
        @Value("${fees.request.max-retries}") int maxRetriesFeesAndPay,
        @Value("${fees.request.backoff-delay-seconds}") Duration feesAndPayBackoffDelay
    ) {
        this.feeService = feeService;
        this.paymentService = paymentService;
        this.maxRetriesFeesAndPay = maxRetriesFeesAndPay;
        this.feesAndPayBackoffDelay = feesAndPayBackoffDelay;
    }

    /**
     * Creates a scheduled task for creating a payment service request. On successful completion, the task
     * removes itself from the scheduler. On failure, the task will be retried with
     * exponential backoff.
     *
     * @return CustomTask configured with retry logic and exponential backoff on failure
     */
    @Bean
    public CustomTask<FeesAndPayTaskData> feePaymentTask() {
        return Tasks.custom(FEE_CASE_ISSUED_TASK_DESCRIPTOR)
            .onFailure(new FailureHandler.MaxRetriesFailureHandler<>(
                maxRetriesFeesAndPay,
                new FailureHandler.ExponentialBackoffFailureHandler<>(feesAndPayBackoffDelay)
            ))
            .execute((taskInstance, executionContext) -> {
                FeesAndPayTaskData taskData = taskInstance.getData();
                log.debug("Executing fee service request for fee type: {}", taskData.getFeeType());

                FeeDetails feeDetails = taskData.getFeeDetails();

                try {
                    paymentService.createServiceRequest(
                        taskData.getCaseReference(),
                        taskData.getCcdCaseNumber(),
                        feeDetails,
                        taskData.getVolume(),
                        taskData.getResponsibleParty()
                    );

                    return new CompletionHandler.OnCompleteRemove<>();

                } catch (Exception e) {
                    log.error("Failed to create fee service request for type: {}. Attempt {}/{}",
                                taskData.getFeeType(),
                                executionContext.getExecution().consecutiveFailures + 1,
                                maxRetriesFeesAndPay,
                                e);
                    throw e;
                }
            });
    }
}
