package uk.gov.hmcts.reform.pcs.ccd.task;

import com.github.kagkarlsson.scheduler.task.CompletionHandler;
import com.github.kagkarlsson.scheduler.task.ExecutionContext;
import com.github.kagkarlsson.scheduler.task.TaskInstance;
import com.github.kagkarlsson.scheduler.task.helper.CustomTask;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.pcs.ccd.model.DefendantResponseStatusChangeTaskData;
import uk.gov.hmcts.reform.pcs.notify.service.DefendantResponseNotificationService;

import java.time.Duration;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.pcs.ccd.task.DefendantResponseSubmittedNotificationTaskComponent.DEFENDANT_RESPONSE_SUBMITTED_TASK_DESCRIPTOR;

@ExtendWith(MockitoExtension.class)
class DefendantResponseSubmittedNotificationTaskComponentTest {

    private static final int MAX_RETRIES = 3;
    private static final Duration BACKOFF_DELAY = Duration.ofSeconds(10);

    @Mock
    private DefendantResponseNotificationService defendantResponseNotificationService;

    @Mock
    private TaskInstance<DefendantResponseStatusChangeTaskData> taskInstance;

    @Mock
    private ExecutionContext executionContext;

    private DefendantResponseSubmittedNotificationTaskComponent underTest;

    @BeforeEach
    void setUp() {
        underTest = new DefendantResponseSubmittedNotificationTaskComponent(
            defendantResponseNotificationService,
            MAX_RETRIES,
            BACKOFF_DELAY
        );
    }

    @Test
    @DisplayName("Should create task descriptor with correct name and type")
    void shouldCreateTaskDescriptorWithCorrectNameAndType() {
        assertThat(DEFENDANT_RESPONSE_SUBMITTED_TASK_DESCRIPTOR.getTaskName())
            .isEqualTo("defendant-response-submitted-task");
        assertThat(DEFENDANT_RESPONSE_SUBMITTED_TASK_DESCRIPTOR.getDataClass())
            .isEqualTo(DefendantResponseStatusChangeTaskData.class);
    }

    @Test
    @DisplayName("Should send notification when task executes")
    void shouldSendNotificationWhenTaskExecutes() {
        UUID defendantResponseId = UUID.randomUUID();
        DefendantResponseStatusChangeTaskData taskData = DefendantResponseStatusChangeTaskData.builder()
            .defendantResponseId(defendantResponseId)
            .build();
        when(taskInstance.getData()).thenReturn(taskData);

        CustomTask<DefendantResponseStatusChangeTaskData> task = underTest.defendantResponseSubmittedNotificationTask();
        CompletionHandler<DefendantResponseStatusChangeTaskData> completionHandler =
            task.execute(taskInstance, executionContext);

        assertThat(completionHandler).isInstanceOf(CompletionHandler.OnCompleteRemove.class);
        verify(defendantResponseNotificationService).sendEmailNotificationForNoCounterClaim(defendantResponseId);
    }

    @Test
    @DisplayName("Should rethrow exception when notification service fails")
    void shouldRethrowExceptionWhenNotificationServiceFails() {
        UUID defendantResponseId = UUID.randomUUID();
        DefendantResponseStatusChangeTaskData taskData = DefendantResponseStatusChangeTaskData.builder()
            .defendantResponseId(defendantResponseId)
            .build();
        when(taskInstance.getData()).thenReturn(taskData);

        doThrow(new RuntimeException("Service failure")).when(defendantResponseNotificationService)
            .sendEmailNotificationForNoCounterClaim(defendantResponseId);

        CustomTask<DefendantResponseStatusChangeTaskData> task = underTest.defendantResponseSubmittedNotificationTask();

        assertThatThrownBy(() -> task.execute(taskInstance, executionContext))
            .isInstanceOf(RuntimeException.class)
            .hasMessage("Service failure");
    }
}
