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
import uk.gov.hmcts.reform.pcs.feesandpay.entity.Fee;
import uk.gov.hmcts.reform.pcs.feesandpay.service.FeesAndPayService;

import java.time.Duration;

@Slf4j
@Component
public class FeesAndPayTaskComponent {

    private static final String FEES_AND_PAY_CASE_ISSUED_TASK_NAME = "fees-and-pay-task";

    public static final TaskDescriptor<String> FEE_CASE_ISSUED_TASK_DESCRIPTOR =
        TaskDescriptor.of(FEES_AND_PAY_CASE_ISSUED_TASK_NAME, String.class);

    private final FeesAndPayService feesAndPayService;
    private final int maxRetriesFeesAndPay;
    private final Duration feesAndPayBackoffDelay;

    public FeesAndPayTaskComponent(
        FeesAndPayService feesAndPayService,
        @Value("${fees-register.request.max-retries}") int maxRetriesFeesAndPay,
        @Value("${fees-register.request.backoff-delay-seconds}") Duration feesAndPayBackoffDelay
    ) {
        this.feesAndPayService = feesAndPayService;
        this.maxRetriesFeesAndPay = maxRetriesFeesAndPay;
        this.feesAndPayBackoffDelay = feesAndPayBackoffDelay;
    }

    /**
     * Creates a scheduled task for fetching fees from the Fees Register API.
     * The task accepts a fee type string as data, allowing support for multiple fee types.
     *
     * @return CustomTask configured with retry logic and automatic removal on completion
     */
    @Bean
    public CustomTask<String> feesAndPayCaseIssuedTask() {
        return Tasks.custom(FEE_CASE_ISSUED_TASK_DESCRIPTOR)
            .onFailure(new FailureHandler.MaxRetriesFailureHandler<>(
                maxRetriesFeesAndPay,
                new FailureHandler.ExponentialBackoffFailureHandler<>(feesAndPayBackoffDelay)
            ))
            .execute((taskInstance, executionContext) -> {
                String feeType = taskInstance.getData();
                log.debug("Executing fee lookup task for fee type: {}", feeType);

                try {
                    Fee fee = feesAndPayService.getFee(feeType);
                    log.info("Successfully retrieved fee: type={}, code={}, amount={}",
                                feeType, fee.getCode(), fee.getCalculatedAmount());

                    return new CompletionHandler.OnCompleteRemove<>();

                } catch (Exception e) {
                    log.error("Failed to retrieve fee for type: {}. Attempt {}/{}",
                                feeType,
                                executionContext.getExecution().consecutiveFailures + 1,
                                maxRetriesFeesAndPay,
                                e);
                    throw e;
                }
            });
    }
}
