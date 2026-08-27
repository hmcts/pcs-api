package uk.gov.hmcts.reform.pcs.camunda;

import com.github.kagkarlsson.scheduler.task.CompletionHandler;
import com.github.kagkarlsson.scheduler.task.FailureHandler;
import com.github.kagkarlsson.scheduler.task.TaskDescriptor;
import com.github.kagkarlsson.scheduler.task.helper.CustomTask;
import com.github.kagkarlsson.scheduler.task.helper.Tasks;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Slf4j
@Component
public class CamundaRequestTaskComponent {

    private static final String CAMUNDA_REQUEST_TASK_NAME = "camunda-request-task";

    public static final TaskDescriptor<CamundaRequestTaskData> CAMUNDA_REQUEST_TASK_DESCRIPTOR =
        TaskDescriptor.of(CAMUNDA_REQUEST_TASK_NAME, CamundaRequestTaskData.class);

    private final CamundaService camundaService;
    private final int maxRetries;
    private final Duration backoffDelay;

    public CamundaRequestTaskComponent(CamundaService camundaService,
                                       @Value("${camunda.request.max-retries}") int maxRetries,
                                       @Value("${camunda.request.backoff-delay-duration}") Duration backoffDelay) {

        this.camundaService = camundaService;
        this.maxRetries = maxRetries;
        this.backoffDelay = backoffDelay;
    }

    @Bean
    public CustomTask<CamundaRequestTaskData> camundaRequestTask() {
        return Tasks.custom(CAMUNDA_REQUEST_TASK_DESCRIPTOR)
            .onFailure(
                FailureHandler.<CamundaRequestTaskData>maxRetries(maxRetries)
                    .withBackoff(backoffDelay)
                    .thenRemove(complete ->
                                    log.warn(
                                        "Execution has failed {} times. Giving up.",
                                        complete.getExecution().consecutiveFailures
                                    )
            ))
            .execute((taskInstance, executionContext) -> {
                CamundaRequestTaskData taskData = taskInstance.getData();
                log.debug("Executing Camunda request task for {}", taskData);
                try {
                    camundaService.handleRequest(taskData);
                    return new CompletionHandler.OnCompleteRemove<>();
                } catch (Exception e) {
                    log.error("Failed to create Camunda request for: {}. Attempt {}/{}",
                              taskData, executionContext.getExecution().consecutiveFailures + 1, maxRetries, e);
                    throw e;
                }
            });
    }

}
