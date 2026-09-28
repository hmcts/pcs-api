package uk.gov.hmcts.reform.pcs.ccd.task;


import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.github.kagkarlsson.scheduler.SchedulerClient;
import com.github.kagkarlsson.scheduler.task.CompletionHandler;
import com.github.kagkarlsson.scheduler.task.Execution;
import com.github.kagkarlsson.scheduler.task.ExecutionContext;
import com.github.kagkarlsson.scheduler.task.SchedulableInstance;
import com.github.kagkarlsson.scheduler.task.TaskInstance;
import com.github.kagkarlsson.scheduler.task.helper.CustomTask;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;
import uk.gov.hmcts.reform.pcs.ccd.model.AccessCodeTaskData;
import uk.gov.hmcts.reform.pcs.ccd.service.DefendantAccessCodeService;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mock.Strictness.LENIENT;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.pcs.ccd.task.AccessCodeGenerationComponent.ACCESS_CODE_TASK_DESCRIPTOR;

@ExtendWith(MockitoExtension.class)
class AccessCodeGenerationComponentTest {

    private static final String CASE_REFERENCE = "1234567812345678";
    private static final UUID DEFENDANT_PARTY_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");

    private AccessCodeGenerationComponent accessCodeGenerationComponent;

    @Mock
    private DefendantAccessCodeService defendantAccessCodeService;

    @Mock
    private TaskInstance<AccessCodeTaskData> taskInstance;

    @Mock
    private ExecutionContext executionContext;

    @Mock
    private Execution execution;

    @Mock
    private SchedulerClient schedulerClient;

    @Mock(strictness = LENIENT)
    private Clock utcClock;

    @Captor
    private ArgumentCaptor<SchedulableInstance<SendClaimPackTaskData>> schedulableInstanceCaptor;

    private final Duration backoffDelay = Duration.ofSeconds(3);

    private final int maxRetries = 5;

    private Logger componentLogger;
    private ListAppender<ILoggingEvent> logAppender;

    private static final LocalDateTime TEST_UTC_DATE_TIME = LocalDate.of(2025, 8, 27)
        .atTime(12, 51, 19);

    @BeforeEach
    void setUp() {
        accessCodeGenerationComponent = new AccessCodeGenerationComponent(
            defendantAccessCodeService,
            maxRetries,
            backoffDelay,
            schedulerClient,
            utcClock
        );

        componentLogger = (Logger) LoggerFactory.getLogger(AccessCodeGenerationComponent.class);
        logAppender = new ListAppender<>();
        logAppender.start();
        componentLogger.addAppender(logAppender);
    }

    @AfterEach
    void tearDown() {
        componentLogger.detachAppender(logAppender);
    }

    private static AccessCodeTaskData taskData() {
        return AccessCodeTaskData.builder()
            .caseReference(CASE_REFERENCE)
            .defendantPartyId(DEFENDANT_PARTY_ID.toString())
            .sendClaimPack(false)
            .build();
    }

    @Test
    @DisplayName("Should create task descriptor with correct name and type")
    void shouldCreateTaskDescriptorWithCorrectNameAndType() {
        assertThat(ACCESS_CODE_TASK_DESCRIPTOR.getTaskName())
            .isEqualTo("access-code-generation-task");
        assertThat(ACCESS_CODE_TASK_DESCRIPTOR.getDataClass())
            .isEqualTo(AccessCodeTaskData.class);
    }

    @Test
    @DisplayName("Should execute task and generate the access code for the task's defendant")
    void shouldExecuteTaskAndGenerateForDefendant() {
        //Given
        when(taskInstance.getData()).thenReturn(taskData());
        when(executionContext.getExecution()).thenReturn(execution);
        CustomTask<AccessCodeTaskData> task = accessCodeGenerationComponent.accessCodeGenerationTask();

        //When
        CompletionHandler<AccessCodeTaskData> result = task.execute(taskInstance, executionContext);

        //Then
        verify(defendantAccessCodeService).generateForDefendant(1234567812345678L, DEFENDANT_PARTY_ID, true, false);

        assertThat(result).isInstanceOf(CompletionHandler.OnCompleteRemove.class);

        verifyNoInteractions(schedulerClient);
    }

    @Test
    @DisplayName("Final attempt stamps partyId (plus the standard dimensions) on the terminal ERROR")
    void finalAttemptStampsPartyIdOnTerminalError() {
        when(taskInstance.getData()).thenReturn(taskData());
        execution.consecutiveFailures = maxRetries;
        when(executionContext.getExecution()).thenReturn(execution);
        doThrow(new RuntimeException("docassembly 500"))
            .when(defendantAccessCodeService).generateForDefendant(1234567812345678L, DEFENDANT_PARTY_ID, false, true);

        CustomTask<AccessCodeTaskData> task = accessCodeGenerationComponent.accessCodeGenerationTask();
        assertThatThrownBy(() -> task.execute(taskInstance, executionContext))
            .isInstanceOf(RuntimeException.class);

        List<ILoggingEvent> terminalErrors = logAppender.list.stream()
            .filter(e -> e.getLevel() == Level.ERROR)
            .filter(e -> e.getFormattedMessage().contains("permanently failed"))
            .toList();
        assertThat(terminalErrors).hasSize(1);

        assertThat(terminalErrors.getFirst().getMDCPropertyMap())
            .containsEntry("caseReference", CASE_REFERENCE)
            .containsEntry("taskName", "access-code-generation-task")
            .containsEntry("terminalFailure", "true")
            .containsEntry("failureReason", "docassembly 500")
            .containsEntry("partyId", DEFENDANT_PARTY_ID.toString());

        verifyNoInteractions(schedulerClient);
    }

    @Test
    @DisplayName("Should rethrow exception when generation fails")
    void shouldRetryOnFailure() {
        //Given
        when(taskInstance.getData()).thenReturn(taskData());
        when(executionContext.getExecution()).thenReturn(execution);

        doThrow(mock(RuntimeException.class))
            .when(defendantAccessCodeService)
            .generateForDefendant(eq(1234567812345678L), eq(DEFENDANT_PARTY_ID), anyBoolean(), anyBoolean());

        //When
        CustomTask<AccessCodeTaskData> task = accessCodeGenerationComponent.accessCodeGenerationTask();

        //Then
        assertThatThrownBy(() -> task.execute(taskInstance, executionContext))
            .isInstanceOf(RuntimeException.class);

        verify(defendantAccessCodeService).generateForDefendant(eq(1234567812345678L), eq(DEFENDANT_PARTY_ID),
                                                                anyBoolean(), anyBoolean());

        verifyNoInteractions(schedulerClient);
    }

    @Test
    void shouldTriggerClaimPackTask() {
        //Given
        AccessCodeTaskData accessCodeTaskData = AccessCodeTaskData.builder()
            .caseReference(CASE_REFERENCE)
            .defendantPartyId(DEFENDANT_PARTY_ID.toString())
            .sendClaimPack(true)
            .build();


        when(taskInstance.getData()).thenReturn(accessCodeTaskData);
        when(executionContext.getExecution()).thenReturn(execution);

        when(utcClock.instant()).thenReturn(TEST_UTC_DATE_TIME.toInstant(ZoneOffset.UTC));
        when(utcClock.getZone()).thenReturn(ZoneOffset.UTC);

        CustomTask<AccessCodeTaskData> task = accessCodeGenerationComponent.accessCodeGenerationTask();

        //When
        CompletionHandler<AccessCodeTaskData> result = task.execute(taskInstance, executionContext);

        //Then
        verify(defendantAccessCodeService).generateForDefendant(1234567812345678L, DEFENDANT_PARTY_ID, true, false);

        assertThat(result).isInstanceOf(CompletionHandler.OnCompleteRemove.class);

        verify(schedulerClient).scheduleIfNotExists(schedulableInstanceCaptor.capture());
        SchedulableInstance<SendClaimPackTaskData> schedulableInstance = schedulableInstanceCaptor.getValue();
        SendClaimPackTaskData sendClaimPackTaskData = schedulableInstance.getTaskInstance().getData();
        assertThat(sendClaimPackTaskData.getCaseReference()).isEqualTo(1234567812345678L);
        assertThat(schedulableInstance.getNextExecutionTime(Instant.now()))
            .isEqualTo(Instant.parse("2025-08-27T12:51:19Z"));
    }

}
