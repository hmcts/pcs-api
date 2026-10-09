package uk.gov.hmcts.reform.pcs.ccd.task;

import com.github.kagkarlsson.scheduler.task.CompletionHandler;
import com.github.kagkarlsson.scheduler.task.FailureHandler;
import com.github.kagkarlsson.scheduler.task.TaskDescriptor;
import com.github.kagkarlsson.scheduler.task.helper.CustomTask;
import com.github.kagkarlsson.scheduler.task.helper.Tasks;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.pcs.ccd.domain.DocumentType;
import uk.gov.hmcts.reform.pcs.ccd.domain.claimactivitylog.GenerationDetails;
import uk.gov.hmcts.reform.pcs.ccd.model.HearingNoticeTaskData;
import uk.gov.hmcts.reform.pcs.ccd.service.claimform.ClaimActivityLogService;
import uk.gov.hmcts.reform.pcs.ccd.service.hearing.HearingNoticeDocumentGenerator;

import java.time.Duration;

@Slf4j
@Component
public class HearingNoticeGenerationComponent {

    private static final String HEARING_NOTICE_GENERATION_TASK_NAME = "hearing-notice-generation-task";

    private static final String MDC_CASE_REFERENCE = "caseReference";
    private static final String MDC_HEARING_ID = "hearingId";
    private static final String MDC_TASK_NAME = "taskName";
    private static final String MDC_TERMINAL_FAILURE = "terminalFailure";
    private static final String MDC_FAILURE_REASON = "failureReason";

    public static final TaskDescriptor<HearingNoticeTaskData> HEARING_NOTICE_TASK_DESCRIPTOR =
        TaskDescriptor.of(HEARING_NOTICE_GENERATION_TASK_NAME, HearingNoticeTaskData.class);

    private final HearingNoticeDocumentGenerator hearingNoticeDocumentGenerator;
    private final ClaimActivityLogService claimActivityLogService;
    private final int maxRetries;
    private final Duration backoffDelay;

    public HearingNoticeGenerationComponent(
        HearingNoticeDocumentGenerator hearingNoticeDocumentGenerator,
        ClaimActivityLogService claimActivityLogService,
        @Value("${hearing-notice.request.max-retries}") int maxRetries,
        @Value("${hearing-notice.request.backoff-delay-seconds}") Duration backoffDelay
    ) {
        this.hearingNoticeDocumentGenerator = hearingNoticeDocumentGenerator;
        this.claimActivityLogService = claimActivityLogService;
        this.maxRetries = maxRetries;
        this.backoffDelay = backoffDelay;
    }

    @Bean
    public CustomTask<HearingNoticeTaskData> hearingNoticeGenerationTask() {
        return Tasks.custom(HEARING_NOTICE_TASK_DESCRIPTOR)
            .onFailure(new FailureHandler.MaxRetriesFailureHandler<>(
                maxRetries,
                new FailureHandler.ExponentialBackoffFailureHandler<>(backoffDelay)
            ))
            .execute((taskInstance, executionContext) -> {
                HearingNoticeTaskData data = taskInstance.getData();
                Integer hearingId = data.getHearingId();
                MDC.put(MDC_CASE_REFERENCE, String.valueOf(data.getCaseReference()));
                MDC.put(MDC_HEARING_ID, String.valueOf(hearingId));
                MDC.put(MDC_TASK_NAME, HEARING_NOTICE_GENERATION_TASK_NAME);

                try {
                    hearingNoticeDocumentGenerator.generateNotices(hearingId);
                    return new CompletionHandler.OnCompleteRemove<>();
                } catch (Exception e) {
                    int attempt = executionContext.getExecution().consecutiveFailures + 1;
                    boolean finalAttempt = isFinalAttempt(attempt);
                    if (finalAttempt) {
                        MDC.put(MDC_TERMINAL_FAILURE, "true");
                        MDC.put(MDC_FAILURE_REASON, e.getClass().getSimpleName());
                        log.error("Hearing notice generation permanently failed for hearing {} after {} attempts",
                            hearingId, attempt, e);
                    }
                    // First + terminal rows: reason visible from attempt 1 (terminal:false = retrying)
                    if (attempt == 1 || finalAttempt) {
                        recordGenerationFailure(data, e, finalAttempt);
                    }
                    throw e;
                } finally {
                    MDC.remove(MDC_CASE_REFERENCE);
                    MDC.remove(MDC_HEARING_ID);
                    MDC.remove(MDC_TASK_NAME);
                    MDC.remove(MDC_TERMINAL_FAILURE);
                    MDC.remove(MDC_FAILURE_REASON);
                }
            });
    }

    private boolean isFinalAttempt(int attempt) {
        return attempt > maxRetries;
    }

    private void recordGenerationFailure(HearingNoticeTaskData data, Exception cause, boolean terminal) {
        try {
            claimActivityLogService.logGenerationFailure(data.getCaseReference(), null,
                GenerationDetails.forFailure(DocumentType.HEARING_NOTICE, cause, terminal));
        } catch (Exception e) {
            log.error("Failed to record hearing notice generation failure for hearing {}", data.getHearingId(), e);
        }
    }
}
