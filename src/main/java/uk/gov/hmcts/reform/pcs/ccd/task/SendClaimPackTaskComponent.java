package uk.gov.hmcts.reform.pcs.ccd.task;

import com.github.kagkarlsson.scheduler.task.CompletionHandler;
import com.github.kagkarlsson.scheduler.task.TaskDescriptor;
import com.github.kagkarlsson.scheduler.task.helper.CustomTask;
import com.github.kagkarlsson.scheduler.task.helper.Tasks;
import com.github.kagkarlsson.scheduler.task.schedule.FixedDelay;
import com.github.kagkarlsson.scheduler.task.schedule.Schedule;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.pcs.ccd.service.bulkprint.ClaimPackSender;

import java.time.Duration;

@Slf4j
@Component
public class SendClaimPackTaskComponent {

    private final ClaimPackSender claimPackSender;

    private static final Schedule RETRY_SCHEDULE = FixedDelay.of(Duration.ofMinutes(5));

    private static final String SEND_CLAIM_PACK_TASK_NAME = "send-claim-task";

    public static final TaskDescriptor<SendClaimPackTaskData> SEND_CLAIM_PACK_TASK_DESCRIPTOR =
        TaskDescriptor.of(SEND_CLAIM_PACK_TASK_NAME, SendClaimPackTaskData.class);

    public SendClaimPackTaskComponent(ClaimPackSender claimPackSender) {
        this.claimPackSender = claimPackSender;
    }

    @Bean
    public CustomTask<SendClaimPackTaskData> sendClaimPackTask() {
        return Tasks.custom(SEND_CLAIM_PACK_TASK_DESCRIPTOR)
            .onFailureReschedule(RETRY_SCHEDULE)
            .execute((taskInstance, executionContext) -> {
                SendClaimPackTaskData taskData = taskInstance.getData();
                try {
                    claimPackSender.sendClaimPacks(taskData.getCaseId());
                    return new CompletionHandler.OnCompleteRemove<>();
                } catch (Exception e) {
                    log.error("Failed to send claim pack for {}", taskData.getCaseId(), e);
                    throw e;
                }
            });
    }
}
