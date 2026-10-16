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
import uk.gov.hmcts.reform.pcs.ccd.event.service.CcdPaymentStateUpdateService;
import uk.gov.hmcts.reform.pcs.feesandpay.model.CcdPaymentStateUpdateTaskData;
import uk.gov.hmcts.reform.pcs.feesandpay.model.FeesAndPayTaskData;

import java.time.Duration;

@Slf4j
@Component
public class CcdPaymentUpdateTask {

    private static final String CCD_PAYMENT_UPDATE_TASK = "ccd-payment-update-task";

    public static final TaskDescriptor<CcdPaymentStateUpdateTaskData> CCD_PAYMENT_DATE_TASK_DESCRIPTOR =
        TaskDescriptor.of(CCD_PAYMENT_UPDATE_TASK, CcdPaymentStateUpdateTaskData.class);

    private final CcdPaymentStateUpdateService ccdPaymentStateUpdateService;
    private final int maxRetriesCcdPaymentUpdate;
    private final Duration ccdPaymentUpdateBackoffDelay;

    public CcdPaymentUpdateTask(CcdPaymentStateUpdateService ccdPaymentStateUpdateService,
        @Value("${ccd.payment.update.max-retries}") int maxRetriesCcdPaymentUpdate,
        @Value("${ccd.payment.update.backoff-delay-seconds}") Duration ccdPaymentUpdateBackoffDelay) {
        this.ccdPaymentStateUpdateService = ccdPaymentStateUpdateService;
        this.maxRetriesCcdPaymentUpdate = maxRetriesCcdPaymentUpdate;
        this.ccdPaymentUpdateBackoffDelay = ccdPaymentUpdateBackoffDelay;
    }

    @Bean
    public CustomTask<CcdPaymentStateUpdateTaskData> ccdPaymentUpdateTask() {
        return Tasks.custom(CCD_PAYMENT_DATE_TASK_DESCRIPTOR)
            .onFailure(new FailureHandler.MaxRetriesFailureHandler<>(maxRetriesCcdPaymentUpdate,
                new FailureHandler.ExponentialBackoffFailureHandler<>(ccdPaymentUpdateBackoffDelay)
            ))
            .execute((taskInstance, executionContext) -> {
                CcdPaymentStateUpdateTaskData taskData = taskInstance.getData();
                log.debug("Executing CCD Payment Update for case: {} and event: {}",
                          taskData.getCaseReference(), taskData.getEventId());
                try {
                    ccdPaymentStateUpdateService.updateCcd(taskData);
                    return new CompletionHandler.OnCompleteRemove<>();
                } catch (Exception e) {
                    log.error("Failed to update CCD : {}. Attempt {}/{}", taskData,
                              executionContext.getExecution().consecutiveFailures + 1, maxRetriesCcdPaymentUpdate, e);
                    throw e;
                }
            });
    }

}
