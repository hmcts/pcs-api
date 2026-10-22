package uk.gov.hmcts.reform.pcs.enforcement.document;

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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;
import uk.gov.hmcts.reform.pcs.ccd.domain.DocumentType;
import uk.gov.hmcts.reform.pcs.ccd.domain.claimactivitylog.GenerationDetails;
import uk.gov.hmcts.reform.pcs.ccd.service.claimform.ClaimActivityLogService;
import uk.gov.hmcts.reform.pcs.enforcement.repository.EnforcementOrderRepository;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.pcs.enforcement.document.EnforcementDocumentGenerationComponent.ENFORCEMENT_DOCUMENT_TASK_DESCRIPTOR;

@ExtendWith(MockitoExtension.class)
class EnforcementDocumentGenerationComponentTest {

    private static final UUID ENFORCEMENT_ID = UUID.randomUUID();
    private static final long CASE_REFERENCE = 1234567890123456L;
    private static final int MAX_RETRIES = 5;

    @Mock
    private EnforcementDocumentGenerator generator;
    @Mock
    private EnforcementOrderRepository enforcementOrderRepository;
    @Mock
    private ClaimActivityLogService claimActivityLogService;
    @Mock
    private TaskInstance<EnforcementDocumentTaskData> taskInstance;
    @Mock
    private ExecutionContext executionContext;
    @Mock
    private Execution execution;

    private CustomTask<EnforcementDocumentTaskData> task;
    private Logger componentLogger;
    private ListAppender<ILoggingEvent> logAppender;

    @BeforeEach
    void setUp() {
        task = new EnforcementDocumentGenerationComponent(generator, enforcementOrderRepository,
                                                          claimActivityLogService, MAX_RETRIES, Duration.ofSeconds(3))
            .enforcementDocumentGenerationTask();
        lenient().when(taskInstance.getData()).thenReturn(
            new EnforcementDocumentTaskData(ENFORCEMENT_ID, EnforcementOutput.WARRANT_OF_POSSESSION));
        lenient().when(enforcementOrderRepository.findCaseReferenceById(ENFORCEMENT_ID))
            .thenReturn(Optional.of(CASE_REFERENCE));

        componentLogger = (Logger) LoggerFactory.getLogger(EnforcementDocumentGenerationComponent.class);
        logAppender = new ListAppender<>();
        logAppender.start();
        componentLogger.addAppender(logAppender);
    }

    @AfterEach
    void tearDown() {
        componentLogger.detachAppender(logAppender);
    }

    @Test
    void shouldNameTheTask() {
        assertThat(ENFORCEMENT_DOCUMENT_TASK_DESCRIPTOR.getTaskName())
            .isEqualTo("enforcement-document-generation-task");
        assertThat(ENFORCEMENT_DOCUMENT_TASK_DESCRIPTOR.getDataClass()).isEqualTo(EnforcementDocumentTaskData.class);
    }

    @Test
    void shouldGenerateTheDocumentAndRemoveTheTask() {
        CompletionHandler<EnforcementDocumentTaskData> result = task.execute(taskInstance, executionContext);

        verify(generator).generate(ENFORCEMENT_ID, EnforcementOutput.WARRANT_OF_POSSESSION);
        assertThat(result).isInstanceOf(CompletionHandler.OnCompleteRemove.class);
        verify(claimActivityLogService, never()).logGenerationFailure(anyLong(), any(GenerationDetails.class));
    }

    @Test
    void shouldRecordTheFirstFailureAsRetrying() {
        givenFailureOnAttempt(1);

        assertThatThrownBy(() -> task.execute(taskInstance, executionContext)).hasMessage("docassembly 500");

        verify(claimActivityLogService).logGenerationFailure(eq(CASE_REFERENCE), argThat(details ->
            details.documentType() == DocumentType.WARRANT_OF_POSSESSION && !details.terminal()));
        assertThat(logAppender.list).noneMatch(event -> event.getLevel() == Level.ERROR);
    }

    @Test
    void shouldRethrowSoTheTaskIsRetriedWithoutRecordingOrLoggingAMiddleFailure() {
        givenFailureOnAttempt(MAX_RETRIES);

        assertThatThrownBy(() -> task.execute(taskInstance, executionContext)).hasMessage("docassembly 500");

        verify(claimActivityLogService, never()).logGenerationFailure(anyLong(), any(GenerationDetails.class));
        assertThat(logAppender.list).noneMatch(event -> event.getLevel() == Level.ERROR);
    }

    @Test
    void shouldRecordAndLogTheTerminalFailureOnTheLastAttempt() {
        givenFailureOnAttempt(MAX_RETRIES + 1);

        assertThatThrownBy(() -> task.execute(taskInstance, executionContext)).isInstanceOf(RuntimeException.class);

        verify(claimActivityLogService).logGenerationFailure(eq(CASE_REFERENCE), argThat(details ->
            details.documentType() == DocumentType.WARRANT_OF_POSSESSION && details.terminal()
                && details.errorDetail().contains("docassembly 500")));
        assertThat(logAppender.list).filteredOn(event -> event.getLevel() == Level.ERROR).singleElement()
            .satisfies(event -> {
                assertThat(event.getFormattedMessage()).contains("permanently failed").contains("1234567890123456");
                assertThat(event.getMDCPropertyMap()).containsAllEntriesOf(Map.of(
                    "terminalFailure", "true",
                    "taskName", "enforcement-document-generation-task",
                    "caseReference", "1234567890123456",
                    "enforcementOutput", "WARRANT_OF_POSSESSION",
                    "enforcementId", ENFORCEMENT_ID.toString(),
                    "failureReason", "RuntimeException"));
            });
    }

    @Test
    void shouldStillLogTheTerminalFailureWhenTheFailureCannotBeRecorded() {
        givenFailureOnAttempt(MAX_RETRIES + 1);
        doThrow(new IllegalStateException("database down"))
            .when(claimActivityLogService).logGenerationFailure(anyLong(), any(GenerationDetails.class));

        assertThatThrownBy(() -> task.execute(taskInstance, executionContext)).hasMessage("docassembly 500");

        assertThat(logAppender.list)
            .anyMatch(event -> event.getLevel() == Level.ERROR
                && "true".equals(event.getMDCPropertyMap().get("terminalFailure")));
    }

    @Test
    void shouldLogTheTerminalFailureWithoutACaseWhenTheEnforcementHasGone() {
        givenFailureOnAttempt(MAX_RETRIES + 1);
        when(enforcementOrderRepository.findCaseReferenceById(ENFORCEMENT_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> task.execute(taskInstance, executionContext)).hasMessage("docassembly 500");

        verify(claimActivityLogService, never()).logGenerationFailure(anyLong(), any(GenerationDetails.class));
        assertThat(logAppender.list)
            .anyMatch(event -> event.getLevel() == Level.ERROR
                && "true".equals(event.getMDCPropertyMap().get("terminalFailure"))
                && !event.getMDCPropertyMap().containsKey("caseReference"));
    }

    private void givenFailureOnAttempt(int attempt) {
        execution.consecutiveFailures = attempt - 1;
        when(executionContext.getExecution()).thenReturn(execution);
        when(generator.generate(ENFORCEMENT_ID, EnforcementOutput.WARRANT_OF_POSSESSION))
            .thenThrow(new RuntimeException("docassembly 500"));
    }
}
