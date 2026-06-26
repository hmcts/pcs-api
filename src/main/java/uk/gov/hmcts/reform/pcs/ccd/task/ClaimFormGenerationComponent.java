package uk.gov.hmcts.reform.pcs.ccd.task;

import com.github.kagkarlsson.scheduler.task.TaskDescriptor;
import com.github.kagkarlsson.scheduler.task.helper.CustomTask;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.pcs.ccd.model.ClaimFormTaskData;
import uk.gov.hmcts.reform.pcs.ccd.service.claimform.ClaimActivityLogService;
import uk.gov.hmcts.reform.pcs.ccd.service.claimform.ClaimFormService;

import java.time.Duration;

/**
 * db-scheduler task for claim form generation. Retry shape, terminal-only logging and App Insights
 * dimensions all live in {@link AbstractGenerationTaskComponent}; this only renders/attaches the form
 * and records a single per-case FAILURE row in {@code claim_activity_log} on terminal failure.
 */
@Component
public class ClaimFormGenerationComponent extends AbstractGenerationTaskComponent<ClaimFormTaskData> {

    private static final String TASK_NAME = "claim-form-generation-task";

    public static final TaskDescriptor<ClaimFormTaskData> CLAIM_FORM_TASK_DESCRIPTOR =
        TaskDescriptor.of(TASK_NAME, ClaimFormTaskData.class);

    private final ClaimFormService claimFormService;
    private final ClaimActivityLogService claimActivityLogService;

    public ClaimFormGenerationComponent(
        ClaimFormService claimFormService,
        ClaimActivityLogService claimActivityLogService,
        @Value("${claim-form.request.max-retries}") int maxRetries,
        @Value("${claim-form.request.backoff-delay-seconds}") Duration backoffDelay
    ) {
        super(maxRetries, backoffDelay);
        this.claimFormService = claimFormService;
        this.claimActivityLogService = claimActivityLogService;
    }

    @Bean
    public CustomTask<ClaimFormTaskData> claimFormGenerationTask() {
        return buildTask();
    }

    @Override
    protected String taskName() {
        return TASK_NAME;
    }

    @Override
    protected TaskDescriptor<ClaimFormTaskData> taskDescriptor() {
        return CLAIM_FORM_TASK_DESCRIPTOR;
    }

    @Override
    protected void generate(long caseReference, boolean finalAttempt) {
        claimFormService.generateAndAttach(caseReference);
    }

    @Override
    protected void recordTerminalFailure(long caseReference) {
        claimActivityLogService.logGenerationFailure(caseReference);
        log.error("Recorded DOCUMENTS_CREATED/FAILURE in claim_activity_log for case {}", caseReference);
    }
}
