package uk.gov.hmcts.reform.pcs.ccd.task;

import com.github.kagkarlsson.scheduler.task.CompletionHandler;
import com.github.kagkarlsson.scheduler.task.FailureHandler;
import com.github.kagkarlsson.scheduler.task.TaskDescriptor;
import com.github.kagkarlsson.scheduler.task.helper.CustomTask;
import com.github.kagkarlsson.scheduler.task.helper.Tasks;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.pcs.ccd.model.CaseReferenceTaskData;
import uk.gov.hmcts.reform.pcs.ccd.service.CcdSupplementaryDataService;

import java.time.Duration;

@Slf4j
@Component
public class SupplementaryDataTaskComponent {

    private static final String SUPPLEMENTARY_DATA_TASK_NAME = "supplementary-data-task";

    public static final TaskDescriptor<CaseReferenceTaskData> SUPPLEMENTARY_DATA_TASK_DESCRIPTOR =
        TaskDescriptor.of(SUPPLEMENTARY_DATA_TASK_NAME, CaseReferenceTaskData.class);

    private final CcdSupplementaryDataService ccdSupplementaryDataService;
    private final int maxRetries;
    private final Duration backoffDelay;

    public SupplementaryDataTaskComponent(
        CcdSupplementaryDataService ccdSupplementaryDataService,
        @Value("${supplementary-data.request.max-retries}") int maxRetries,
        @Value("${supplementary-data.request.backoff-delay-seconds}") Duration backoffDelay
    ) {
        this.ccdSupplementaryDataService = ccdSupplementaryDataService;
        this.maxRetries = maxRetries;
        this.backoffDelay = backoffDelay;
    }

    @Bean
    public CustomTask<CaseReferenceTaskData> supplementaryDataTask() {
        return Tasks.custom(SUPPLEMENTARY_DATA_TASK_DESCRIPTOR)
            .onFailure(new FailureHandler.MaxRetriesFailureHandler<>(
                maxRetries,
                new FailureHandler.ExponentialBackoffFailureHandler<>(backoffDelay)
            ))
            .execute((taskInstance, executionContext) -> {
                String caseReference = taskInstance.getData().getCaseReference();
                log.debug("Submitting supplementary data to CCD for case: {}", caseReference);

                try {
                    ccdSupplementaryDataService.submitSupplementaryDataRequestToCcd(caseReference);
                    return new CompletionHandler.OnCompleteRemove<>();

                } catch (Exception e) {
                    log.error("Supplementary data submission failed for case: {}. Attempt {}/{}",
                              caseReference,
                              executionContext.getExecution().consecutiveFailures + 1,
                              maxRetries,
                              e);
                    throw e;
                }
            });
    }
}
