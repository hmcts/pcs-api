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
import uk.gov.hmcts.reform.pcs.ccd.model.DefendantResponseStatusChangeTaskData;
import uk.gov.hmcts.reform.pcs.notify.service.DefendantResponseNotificationService;

import java.time.Duration;
import java.util.UUID;

@Slf4j
@Component
public class DefendantResponseSubmittedNotificationTaskComponent {
    private static final String DEFENDANT_RESPONSE_SUBMITTED_TASK_NAME = "defendant-response-submitted-task";

    public static final TaskDescriptor<DefendantResponseStatusChangeTaskData>
        DEFENDANT_RESPONSE_SUBMITTED_TASK_DESCRIPTOR = TaskDescriptor.of(DEFENDANT_RESPONSE_SUBMITTED_TASK_NAME,
                                                                         DefendantResponseStatusChangeTaskData.class);

    private final DefendantResponseNotificationService defendantResponseNotificationService;

    private final int maxRetries;
    private final Duration backoffDelay;

    public DefendantResponseSubmittedNotificationTaskComponent(
        DefendantResponseNotificationService defendantResponseNotificationService,
        @Value("${defendant-response-notification.request.max-retries}") int maxRetries,
        @Value("${defendant-response-notification.request.backoff-delay-seconds}") Duration backoffDelay
    ) {
        this.defendantResponseNotificationService = defendantResponseNotificationService;
        this.maxRetries = maxRetries;
        this.backoffDelay = backoffDelay;
    }

    @Bean
    public CustomTask<DefendantResponseStatusChangeTaskData> defendantResponseSubmittedNotificationTask() {
        return Tasks.custom(DEFENDANT_RESPONSE_SUBMITTED_TASK_DESCRIPTOR)
            .onFailure(new FailureHandler.MaxRetriesFailureHandler<>(
                maxRetries,
                new FailureHandler.ExponentialBackoffFailureHandler<>(backoffDelay)
            ))
            .execute((taskInstance, executionContext) -> {
                DefendantResponseStatusChangeTaskData taskData = taskInstance.getData();
                UUID defendantResponseId = taskData.getDefendantResponseId();
                log.info("Processing defendant response submitted notification for: {}", defendantResponseId);

                defendantResponseNotificationService.sendEmailNotificationForNoCounterClaim(defendantResponseId);
                defendantResponseNotificationService.sendDefendantResponseReceived(defendantResponseId);

                return new CompletionHandler.OnCompleteRemove<>();
            });
    }
}
