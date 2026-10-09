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
import uk.gov.hmcts.reform.pcs.ccd.model.GenAppDocumentTaskData;
import uk.gov.hmcts.reform.pcs.ccd.service.genapp.GenAppService;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.pcs.ccd.task.GenAppDocumentGenerationComponent.GEN_APP_DOCUMENT_TASK_DESCRIPTOR;

@ExtendWith(MockitoExtension.class)
class GenAppDocumentGenerationComponentTest {

    private static final UUID GEN_APP_ID = UUID.randomUUID();

    private GenAppDocumentGenerationComponent component;

    @Mock
    private GenAppService genAppService;

    @Mock
    private TaskInstance<GenAppDocumentTaskData> taskInstance;

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
        component = new GenAppDocumentGenerationComponent(genAppService, maxRetries, backoffDelay);

        componentLogger = (Logger) LoggerFactory.getLogger(GenAppDocumentGenerationComponent.class);
        logAppender = new ListAppender<>();
        logAppender.start();
        componentLogger.addAppender(logAppender);
    }

    @AfterEach
    void tearDown() {
        componentLogger.detachAppender(logAppender);
    }

    @Test
    @DisplayName("Task descriptor has correct name and data class")
    void taskDescriptorHasCorrectNameAndDataClass() {
        assertThat(GEN_APP_DOCUMENT_TASK_DESCRIPTOR.getTaskName())
            .isEqualTo("gen-app-document-generation-task");
        assertThat(GEN_APP_DOCUMENT_TASK_DESCRIPTOR.getDataClass())
            .isEqualTo(GenAppDocumentTaskData.class);
    }

    @Test
    @DisplayName("Successful execution calls GenAppService and returns OnCompleteRemove")
    void successfulExecutionReturnsOnCompleteRemove() {
        when(taskInstance.getData()).thenReturn(taskData());

        CustomTask<GenAppDocumentTaskData> task = component.genAppDocumentGenerationTask();
        CompletionHandler<GenAppDocumentTaskData> result = task.execute(taskInstance, executionContext);

        verify(genAppService).generateSubmissionDocument(GEN_APP_ID);
        assertThat(result).isInstanceOf(CompletionHandler.OnCompleteRemove.class);
    }

    @Test
    @DisplayName("Non-final attempt rethrows without logging a terminal failure")
    void nonFinalAttemptRethrowsWithoutTerminalLog() {
        when(taskInstance.getData()).thenReturn(taskData());
        execution.consecutiveFailures = maxRetries - 1;
        when(executionContext.getExecution()).thenReturn(execution);
        doThrow(new RuntimeException("transient")).when(genAppService).generateSubmissionDocument(GEN_APP_ID);

        CustomTask<GenAppDocumentTaskData> task = component.genAppDocumentGenerationTask();

        assertThatThrownBy(() -> task.execute(taskInstance, executionContext))
            .isInstanceOf(RuntimeException.class);
        assertThat(logAppender.list)
            .noneMatch(event -> event.getLevel() == Level.ERROR
                && event.getFormattedMessage().contains("permanently failed"));
    }

    @Test
    @DisplayName("Final attempt logs one terminal ERROR with the exception and MDC dimensions")
    void finalAttemptLogsTerminalErrorWithDimensions() {
        when(taskInstance.getData()).thenReturn(taskData());
        execution.consecutiveFailures = maxRetries;
        when(executionContext.getExecution()).thenReturn(execution);
        doThrow(new RuntimeException("docassembly 500"))
            .when(genAppService).generateSubmissionDocument(GEN_APP_ID);

        CustomTask<GenAppDocumentTaskData> task = component.genAppDocumentGenerationTask();

        assertThatThrownBy(() -> task.execute(taskInstance, executionContext))
            .isInstanceOf(RuntimeException.class);

        List<ILoggingEvent> terminalErrors = logAppender.list.stream()
            .filter(event -> event.getLevel() == Level.ERROR)
            .filter(event -> event.getFormattedMessage().contains("permanently failed"))
            .toList();
        assertThat(terminalErrors).hasSize(1);

        ILoggingEvent event = terminalErrors.getFirst();
        assertThat(event.getFormattedMessage())
            .contains(GEN_APP_ID.toString());
        assertThat(event.getThrowableProxy().getMessage()).isEqualTo("docassembly 500");
        assertThat(event.getMDCPropertyMap())
            .containsEntry("genAppId", GEN_APP_ID.toString())
            .containsEntry("taskName", "gen-app-document-generation-task")
            .containsEntry("terminalFailure", "true")
            .containsEntry("failureReason", "RuntimeException");
    }

    private static GenAppDocumentTaskData taskData() {
        return GenAppDocumentTaskData.builder()
            .genAppId(GEN_APP_ID)
            .build();
    }
}
