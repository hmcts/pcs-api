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
import uk.gov.hmcts.reform.pcs.ccd.model.CounterClaimStatusChangeTaskData;
import uk.gov.hmcts.reform.pcs.notify.service.DefendantResponseNotificationService;

import java.time.Duration;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.pcs.ccd.task.PendingCounterClaimIssuedNotificationTaskComponent.PENDING_COUNTER_CLAIM_ISSUED_TASK_DESCRIPTOR;

@ExtendWith(MockitoExtension.class)
class PendingCounterClaimIssuedNotificationTaskComponentTest {

    private static final int MAX_RETRIES = 3;
    private static final Duration BACKOFF_DELAY = Duration.ofSeconds(10);

    @Mock
    private DefendantResponseNotificationService defendantResponseNotificationService;

    @Mock
    private TaskInstance<CounterClaimStatusChangeTaskData> taskInstance;

    @Mock
    private ExecutionContext executionContext;

    private PendingCounterClaimIssuedNotificationTaskComponent underTest;

    @BeforeEach
    void setUp() {
        underTest = new PendingCounterClaimIssuedNotificationTaskComponent(
            defendantResponseNotificationService,
            MAX_RETRIES,
            BACKOFF_DELAY
        );
    }

    @Test
    @DisplayName("Should create task descriptor with correct name and type")
    void shouldCreateTaskDescriptorWithCorrectNameAndType() {
        assertThat(PENDING_COUNTER_CLAIM_ISSUED_TASK_DESCRIPTOR.getTaskName())
            .isEqualTo("pending-counter-claim-issued-task");
        assertThat(PENDING_COUNTER_CLAIM_ISSUED_TASK_DESCRIPTOR.getDataClass())
            .isEqualTo(CounterClaimStatusChangeTaskData.class);
    }

    @Test
    @DisplayName("Should send notification")
    void shouldSendNotification() {
        UUID counterClaimId = UUID.randomUUID();

        CounterClaimStatusChangeTaskData taskData = CounterClaimStatusChangeTaskData.builder()
            .counterClaimId(counterClaimId)
            .build();
        when(taskInstance.getData()).thenReturn(taskData);

        CustomTask<CounterClaimStatusChangeTaskData> task = underTest.pendingCounterClaimIssuedNotificationTask();
        CompletionHandler<CounterClaimStatusChangeTaskData> completionHandler =
            task.execute(taskInstance, executionContext);

        assertThat(completionHandler).isInstanceOf(CompletionHandler.OnCompleteRemove.class);
        verify(defendantResponseNotificationService).sendPendingCounterClaimIssuedNotification(counterClaimId);
    }
}
