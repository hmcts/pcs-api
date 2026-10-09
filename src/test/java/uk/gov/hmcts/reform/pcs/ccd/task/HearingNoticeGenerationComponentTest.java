package uk.gov.hmcts.reform.pcs.ccd.task;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.github.kagkarlsson.scheduler.task.CompletionHandler;
import com.github.kagkarlsson.scheduler.task.Execution;
import com.github.kagkarlsson.scheduler.task.ExecutionContext;
import com.github.kagkarlsson.scheduler.task.TaskInstance;
import com.github.kagkarlsson.scheduler.task.helper.CustomTask;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;
import uk.gov.hmcts.reform.pcs.ccd.domain.DocumentType;
import uk.gov.hmcts.reform.pcs.ccd.domain.claimactivitylog.GenerationDetails;
import uk.gov.hmcts.reform.pcs.ccd.model.HearingNoticeTaskData;
import uk.gov.hmcts.reform.pcs.ccd.service.claimform.ClaimActivityLogService;
import uk.gov.hmcts.reform.pcs.ccd.service.hearing.HearingNoticeDocumentGenerator;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.pcs.ccd.task.HearingNoticeGenerationComponent.HEARING_NOTICE_TASK_DESCRIPTOR;

@ExtendWith(MockitoExtension.class)
class HearingNoticeGenerationComponentTest {

    private static final long CASE_REFERENCE = 1234567812345678L;
    private static final Integer HEARING_ID = 7;

    private HearingNoticeGenerationComponent component;

    @Mock
    private HearingNoticeDocumentGenerator hearingNoticeDocumentGenerator;

    @Mock
    private ClaimActivityLogService claimActivityLogService;

    @Mock
    private TaskInstance<HearingNoticeTaskData> taskInstance;

    @Mock
    private ExecutionContext executionContext;

    @Mock
    private Execution execution;

    private final Duration backoffDelay = Duration.ofSeconds(3);
    private final int maxRetries = 5;

    private Logger componentLogger;
    private ListAppender<ILoggingEvent> logAppender;

    @BeforeEach
    void setUp() {
        component = new HearingNoticeGenerationComponent(hearingNoticeDocumentGenerator, claimActivityLogService,
            maxRetries, backoffDelay);

        componentLogger = (Logger) LoggerFactory.getLogger(HearingNoticeGenerationComponent.class);
        logAppender = new ListAppender<>();
        logAppender.start();
        componentLogger.addAppender(logAppender);
    }

    @AfterEach
    void tearDown() {
        componentLogger.detachAppender(logAppender);
    }

    @Test
    @DisplayName("Task has the right name and data type")
    void taskDescriptorHasCorrectNameAndDataClass() {
        assertThat(HEARING_NOTICE_TASK_DESCRIPTOR.getTaskName())
            .isEqualTo("hearing-notice-generation-task");
        assertThat(HEARING_NOTICE_TASK_DESCRIPTOR.getDataClass())
            .isEqualTo(HearingNoticeTaskData.class);
    }

    @Test
    @DisplayName("Generates the notices and removes the task when it succeeds")
    void successfulExecutionReturnsOnCompleteRemove() {
        when(taskInstance.getData()).thenReturn(taskData());

        CustomTask<HearingNoticeTaskData> task = component.hearingNoticeGenerationTask();
        CompletionHandler<HearingNoticeTaskData> result = task.execute(taskInstance, executionContext);

        verify(hearingNoticeDocumentGenerator).generateNotices(HEARING_ID);
        assertThat(result).isInstanceOf(CompletionHandler.OnCompleteRemove.class);
    }

    @Test
    @DisplayName("Rethrows without an error log or failure record before the last attempt")
    void nonFinalAttemptRethrowsWithoutTerminalLog() {
        when(taskInstance.getData()).thenReturn(taskData());
        execution.consecutiveFailures = maxRetries - 1;
        when(executionContext.getExecution()).thenReturn(execution);
        doThrow(new RuntimeException("transient")).when(hearingNoticeDocumentGenerator).generateNotices(HEARING_ID);

        CustomTask<HearingNoticeTaskData> task = component.hearingNoticeGenerationTask();

        assertThatThrownBy(() -> task.execute(taskInstance, executionContext))
            .isInstanceOf(RuntimeException.class);
        assertThat(logAppender.list)
            .noneMatch(event -> event.getLevel() == Level.ERROR
                && event.getFormattedMessage().contains("permanently failed"));
        verify(claimActivityLogService, never()).logGenerationFailure(anyLong(), any(), any());
    }

    @Test
    @DisplayName("Logs one error tagged with the case and hearing on the last attempt")
    void finalAttemptLogsTerminalError() {
        when(taskInstance.getData()).thenReturn(taskData());
        execution.consecutiveFailures = maxRetries;
        when(executionContext.getExecution()).thenReturn(execution);
        doThrow(new RuntimeException("docassembly 500"))
            .when(hearingNoticeDocumentGenerator).generateNotices(HEARING_ID);

        CustomTask<HearingNoticeTaskData> task = component.hearingNoticeGenerationTask();

        assertThatThrownBy(() -> task.execute(taskInstance, executionContext))
            .isInstanceOf(RuntimeException.class);

        List<ILoggingEvent> terminalErrors = logAppender.list.stream()
            .filter(event -> event.getLevel() == Level.ERROR)
            .filter(event -> event.getFormattedMessage().contains("permanently failed"))
            .toList();
        assertThat(terminalErrors).hasSize(1);

        ILoggingEvent event = terminalErrors.getFirst();
        assertThat(event.getFormattedMessage())
            .contains(HEARING_ID.toString());
        assertThat(event.getThrowableProxy().getMessage()).isEqualTo("docassembly 500");
        assertThat(event.getMDCPropertyMap())
            .containsEntry("caseReference", String.valueOf(CASE_REFERENCE))
            .containsEntry("hearingId", HEARING_ID.toString())
            .containsEntry("taskName", "hearing-notice-generation-task")
            .containsEntry("terminalFailure", "true")
            .containsEntry("failureReason", "RuntimeException");
    }

    @Test
    @DisplayName("Records a retrying failure on the first attempt")
    void firstAttemptRecordsNonTerminalFailure() {
        when(taskInstance.getData()).thenReturn(taskData());
        when(executionContext.getExecution()).thenReturn(execution); // consecutiveFailures = 0 -> attempt 1
        doThrow(new RuntimeException("transient")).when(hearingNoticeDocumentGenerator).generateNotices(HEARING_ID);

        CustomTask<HearingNoticeTaskData> task = component.hearingNoticeGenerationTask();

        assertThatThrownBy(() -> task.execute(taskInstance, executionContext))
            .isInstanceOf(RuntimeException.class);
        verify(claimActivityLogService).logGenerationFailure(eq(CASE_REFERENCE), isNull(),
            argThat((GenerationDetails details) -> !details.terminal()
                && details.documentType() == DocumentType.HEARING_NOTICE));
    }

    @Test
    @DisplayName("Records a final failure on the last attempt")
    void finalAttemptRecordsFailure() {
        when(taskInstance.getData()).thenReturn(taskData());
        execution.consecutiveFailures = maxRetries;
        when(executionContext.getExecution()).thenReturn(execution);
        doThrow(new RuntimeException("docassembly 500"))
            .when(hearingNoticeDocumentGenerator).generateNotices(HEARING_ID);

        CustomTask<HearingNoticeTaskData> task = component.hearingNoticeGenerationTask();

        assertThatThrownBy(() -> task.execute(taskInstance, executionContext))
            .isInstanceOf(RuntimeException.class);
        verify(claimActivityLogService).logGenerationFailure(eq(CASE_REFERENCE), isNull(),
            argThat(GenerationDetails::terminal));
    }

    @Test
    @DisplayName("Still throws the original error if recording the failure fails")
    void rethrowsOriginalErrorWhenRecordingFailureFails() {
        when(taskInstance.getData()).thenReturn(taskData());
        when(executionContext.getExecution()).thenReturn(execution);
        doThrow(new RuntimeException("generation failed"))
            .when(hearingNoticeDocumentGenerator).generateNotices(HEARING_ID);
        doThrow(new RuntimeException("log write failed"))
            .when(claimActivityLogService).logGenerationFailure(anyLong(), any(), any());

        CustomTask<HearingNoticeTaskData> task = component.hearingNoticeGenerationTask();

        assertThatThrownBy(() -> task.execute(taskInstance, executionContext))
            .isInstanceOf(RuntimeException.class)
            .hasMessage("generation failed");
    }

    private static HearingNoticeTaskData taskData() {
        return HearingNoticeTaskData.builder()
            .caseReference(CASE_REFERENCE)
            .hearingId(HEARING_ID)
            .build();
    }
}
