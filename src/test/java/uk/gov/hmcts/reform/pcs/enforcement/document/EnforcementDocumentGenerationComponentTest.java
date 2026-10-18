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

import java.time.Duration;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.pcs.enforcement.document.EnforcementDocumentGenerationComponent.ENFORCEMENT_DOCUMENT_TASK_DESCRIPTOR;

@ExtendWith(MockitoExtension.class)
class EnforcementDocumentGenerationComponentTest {

    private static final UUID ENFORCEMENT_ID = UUID.randomUUID();
    private static final int MAX_RETRIES = 5;

    @Mock
    private EnforcementDocumentGenerator generator;
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
        task = new EnforcementDocumentGenerationComponent(generator, MAX_RETRIES, Duration.ofSeconds(3))
            .enforcementDocumentGenerationTask();
        lenient().when(taskInstance.getData()).thenReturn(
            new EnforcementDocumentTaskData(ENFORCEMENT_ID, EnforcementOutput.WARRANT_OF_POSSESSION));

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
    }

    @Test
    void shouldRethrowSoTheTaskIsRetriedWithoutLoggingATerminalFailure() {
        execution.consecutiveFailures = MAX_RETRIES - 1;
        when(executionContext.getExecution()).thenReturn(execution);
        when(generator.generate(ENFORCEMENT_ID, EnforcementOutput.WARRANT_OF_POSSESSION))
            .thenThrow(new RuntimeException("docassembly 500"));

        assertThatThrownBy(() -> task.execute(taskInstance, executionContext)).hasMessage("docassembly 500");
        assertThat(logAppender.list).noneMatch(event -> event.getLevel() == Level.ERROR);
    }

    @Test
    void shouldLogTheTerminalFailureOnTheLastAttempt() {
        execution.consecutiveFailures = MAX_RETRIES;
        when(executionContext.getExecution()).thenReturn(execution);
        when(generator.generate(ENFORCEMENT_ID, EnforcementOutput.WARRANT_OF_POSSESSION))
            .thenThrow(new RuntimeException("docassembly 500"));

        assertThatThrownBy(() -> task.execute(taskInstance, executionContext)).isInstanceOf(RuntimeException.class);
        assertThat(logAppender.list)
            .anyMatch(event -> event.getLevel() == Level.ERROR
                && event.getFormattedMessage().contains("permanently failed")
                && "true".equals(event.getMDCPropertyMap().get("terminalFailure")));
    }
}
