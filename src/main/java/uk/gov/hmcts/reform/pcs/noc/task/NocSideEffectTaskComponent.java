package uk.gov.hmcts.reform.pcs.noc.task;

import com.github.kagkarlsson.scheduler.task.CompletionHandler;
import com.github.kagkarlsson.scheduler.task.SchedulableInstance;
import com.github.kagkarlsson.scheduler.task.TaskDescriptor;
import com.github.kagkarlsson.scheduler.task.TaskInstance;
import com.github.kagkarlsson.scheduler.task.helper.CustomTask;
import com.github.kagkarlsson.scheduler.task.helper.Tasks;
import java.time.Duration;
import java.time.Instant;
import lombok.AllArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.pcs.noc.model.NocSideEffectTaskData;
import uk.gov.hmcts.reform.pcs.noc.service.NocSideEffectProcessor;

@Component
@AllArgsConstructor
public class NocSideEffectTaskComponent {

    private static final String TASK_NAME = "noc-side-effect-task";
    private static final Duration RETRY_DELAY = Duration.ofMinutes(5);

    public static final TaskDescriptor<NocSideEffectTaskData> NOC_SIDE_EFFECT_TASK =
        TaskDescriptor.of(TASK_NAME, NocSideEffectTaskData.class);

    private final NocSideEffectProcessor processor;

    @Bean
    public CustomTask<NocSideEffectTaskData> nocSideEffectTask() {
        return Tasks.custom(NOC_SIDE_EFFECT_TASK)
            .execute((taskInstance, executionContext) -> {
                NocSideEffectTaskData data = taskInstance.getData();
                boolean completed = processor.process(data.jobId());

                if (completed) {
                    return new CompletionHandler.OnCompleteRemove<>();
                }

                NocSideEffectTaskData retryData = new NocSideEffectTaskData(data.jobId());
                return new CompletionHandler.OnCompleteReplace<>(
                    currentInstance -> SchedulableInstance.of(
                        new TaskInstance<>(TASK_NAME, currentInstance.getId(), retryData),
                        Instant.now().plus(RETRY_DELAY)
                    )
                );
            });
    }
}
