package uk.gov.hmcts.reform.pcs.accesscode.task;

import com.github.kagkarlsson.scheduler.task.CompletionHandler;
import com.github.kagkarlsson.scheduler.task.FailureHandler;
import com.github.kagkarlsson.scheduler.task.TaskDescriptor;
import com.github.kagkarlsson.scheduler.task.helper.CustomTask;
import com.github.kagkarlsson.scheduler.task.helper.Tasks;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.pcs.accesscode.model.AccessCodeTaskData;
import uk.gov.hmcts.reform.pcs.accesscode.service.AccessCodeService;
import uk.gov.hmcts.reform.pcs.feesandpay.service.PaymentService;

import java.time.Duration;

@Slf4j
@Component
public class AccessCodeGenerationComponent {
    private static final String ACCESS_CODE_GENERATION_TASK_NAME = "access-code-generation-task";

    public static final TaskDescriptor<AccessCodeTaskData> ACCESS_CODE_TASK_DESCRIPTOR =
        TaskDescriptor.of(ACCESS_CODE_GENERATION_TASK_NAME, AccessCodeTaskData.class);

    private final AccessCodeService accessCodeService;
    private final int maxRetriesFeesAndPay;
    private final Duration feesAndPayBackoffDelay;

    public AccessCodeGenerationComponent(
        AccessCodeService accessCodeService,
        PaymentService paymentService,
        @Value("${fees.request.max-retries}") int maxRetriesFeesAndPay,
        @Value("${fees.request.backoff-delay-seconds}") Duration feesAndPayBackoffDelay
    ) {
        this.maxRetriesFeesAndPay = maxRetriesFeesAndPay;
        this.feesAndPayBackoffDelay = feesAndPayBackoffDelay;
        this.accessCodeService = accessCodeService;
    }

    /**
     * Creates a scheduled task for generating access code for parties associated with case. On successful completion,
     * the task removes itself from the scheduler. On failure, the task will be retried with exponential backoff.
     *
     * @return CustomTask configured with retry logic and exponential backoff on failure
     */
    @Bean
    public CustomTask<AccessCodeTaskData> accessCodeGenerationTask() {
        return Tasks.custom(ACCESS_CODE_TASK_DESCRIPTOR)
            .onFailure(new FailureHandler.MaxRetriesFailureHandler<>(
                maxRetriesFeesAndPay,
                new FailureHandler.ExponentialBackoffFailureHandler<>(feesAndPayBackoffDelay)
            ))
            .execute((taskInstance, executionContext) -> {
                AccessCodeTaskData taskData = taskInstance.getData();
                log.debug("Executing Access code generation for parties in case: {}", taskData.getCaseReference());

                String caseReference = taskData.getCaseReference();

                try {
                    accessCodeService.createAccessCodesForParties(caseReference);
                    return new CompletionHandler.OnCompleteRemove<>();

                } catch (Exception e) {
                    log.error("Failed to create access code for parties in case: {}. Attempt {}/{}",
                              taskData.getCaseReference(),
                              executionContext.getExecution().consecutiveFailures + 1,
                              maxRetriesFeesAndPay,
                              e);
                    throw e;
                }
            });
    }
}
