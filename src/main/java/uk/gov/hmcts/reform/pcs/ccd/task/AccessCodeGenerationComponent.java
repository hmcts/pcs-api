package uk.gov.hmcts.reform.pcs.ccd.task;

import com.github.kagkarlsson.scheduler.task.TaskDescriptor;
import com.github.kagkarlsson.scheduler.task.helper.CustomTask;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.pcs.ccd.model.AccessCodeTaskData;
import uk.gov.hmcts.reform.pcs.ccd.service.DefendantAccessCodeService;

import java.time.Duration;
import java.util.UUID;

/**
 * db-scheduler task for defendant access-code letter generation. Retry shape, terminal-only logging
 * and App Insights dimensions all live in {@link AbstractGenerationTaskComponent}. One task is scheduled
 * per defendant (instance = {@code caseRef:partyId}), so each defendant retries independently and a
 * per-defendant FAILURE row is written by the service on its own final attempt.
 */
@Component
public class AccessCodeGenerationComponent extends AbstractGenerationTaskComponent<AccessCodeTaskData> {

    private static final String TASK_NAME = "access-code-generation-task";

    public static final TaskDescriptor<AccessCodeTaskData> ACCESS_CODE_TASK_DESCRIPTOR =
        TaskDescriptor.of(TASK_NAME, AccessCodeTaskData.class);

    private final DefendantAccessCodeService defendantAccessCodeService;

    public AccessCodeGenerationComponent(
        DefendantAccessCodeService defendantAccessCodeService,
        @Value("${access-code.request.max-retries}") int maxRetries,
        @Value("${access-code.request.backoff-delay-seconds}") Duration backoffDelay
    ) {
        super(maxRetries, backoffDelay);
        this.defendantAccessCodeService = defendantAccessCodeService;
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
    protected void generate(AccessCodeTaskData taskData, boolean finalAttempt) {
        long caseReference = Long.parseLong(taskData.getCaseReference());
        UUID defendantPartyId = UUID.fromString(taskData.getDefendantPartyId());
        defendantAccessCodeService.generateForDefendant(caseReference, defendantPartyId, finalAttempt);
    }
}
