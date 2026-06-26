package uk.gov.hmcts.reform.pcs.ccd.task;

import com.github.kagkarlsson.scheduler.task.TaskDescriptor;
import com.github.kagkarlsson.scheduler.task.helper.CustomTask;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.pcs.ccd.model.AccessCodeTaskData;
import uk.gov.hmcts.reform.pcs.ccd.service.AccessCodeGenerationService;

import java.time.Duration;

/**
 * db-scheduler task for defendant access-code letter generation. Retry shape, terminal-only logging
 * and App Insights dimensions all live in {@link AbstractGenerationTaskComponent}. This is one-to-many
 * (one letter per defendant), so per-defendant FAILURE rows are written inside the service on the
 * final attempt rather than as a single per-case row.
 */
@Component
public class AccessCodeGenerationComponent extends AbstractGenerationTaskComponent<AccessCodeTaskData> {

    private static final String TASK_NAME = "access-code-generation-task";

    public static final TaskDescriptor<AccessCodeTaskData> ACCESS_CODE_TASK_DESCRIPTOR =
        TaskDescriptor.of(TASK_NAME, AccessCodeTaskData.class);

    private final AccessCodeGenerationService accessCodeGenerationService;

    public AccessCodeGenerationComponent(
        AccessCodeGenerationService accessCodeGenerationService,
        @Value("${access-code.request.max-retries}") int maxRetries,
        @Value("${access-code.request.backoff-delay-seconds}") Duration backoffDelay
    ) {
        super(maxRetries, backoffDelay);
        this.accessCodeGenerationService = accessCodeGenerationService;
    }

    @Bean
    public CustomTask<AccessCodeTaskData> accessCodeGenerationTask() {
        return buildTask();
    }

    @Override
    protected String taskName() {
        return TASK_NAME;
    }

    @Override
    protected TaskDescriptor<AccessCodeTaskData> taskDescriptor() {
        return ACCESS_CODE_TASK_DESCRIPTOR;
    }

    @Override
    protected void generate(long caseReference, boolean finalAttempt) {
        accessCodeGenerationService.createAccessCodesForParties(String.valueOf(caseReference), finalAttempt);
    }
}
