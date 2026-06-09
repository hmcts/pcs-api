package uk.gov.hmcts.reform.pcs.notify.listener;

import com.github.kagkarlsson.scheduler.SchedulerClient;
import com.github.kagkarlsson.scheduler.task.SchedulableInstance;
import com.github.kagkarlsson.scheduler.task.TaskInstance;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.pcs.ccd.domain.respondpossessionclaim.CounterClaimState;
import uk.gov.hmcts.reform.pcs.ccd.entity.respondpossessionclaim.CounterClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.model.CounterClaimStatusChangeTaskData;
import uk.gov.hmcts.reform.pcs.ccd.task.CounterClaimIssuedNotificationTaskComponent;
import uk.gov.hmcts.reform.pcs.ccd.task.PendingCounterClaimIssuedNotificationTaskComponent;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class CounterClaimEntityListenerTest {

    @Mock
    private SchedulerClient schedulerClient;

    @InjectMocks
    private CounterClaimEntityListener underTest;

    @Test
    void shouldSetPreviousStatusOnPostLoad() {
        CounterClaimEntity entity = new CounterClaimEntity();
        entity.setStatus(CounterClaimState.PENDING_COUNTER_CLAIM_ISSUED);

        underTest.onPostLoad(entity);

        assertEquals(CounterClaimState.PENDING_COUNTER_CLAIM_ISSUED, entity.getPreviousStatus());
    }

    @Test
    void shouldScheduleNotificationOnPostPersistWhenStatusIsPendingCounterClaimIssued() {
        UUID counterClaimId = UUID.randomUUID();
        CounterClaimEntity entity = CounterClaimEntity.builder()
            .id(counterClaimId)
            .status(CounterClaimState.PENDING_COUNTER_CLAIM_ISSUED)
            .build();

        underTest.onPostPersist(entity);

        ArgumentCaptor<SchedulableInstance<?>> taskInstanceCaptor = ArgumentCaptor.forClass(SchedulableInstance.class);
        verify(schedulerClient).scheduleIfNotExists(taskInstanceCaptor.capture());

        SchedulableInstance<?> schedulableInstance = taskInstanceCaptor.getValue();
        TaskInstance<?> taskInstance = schedulableInstance.getTaskInstance();
        assertEquals(PendingCounterClaimIssuedNotificationTaskComponent.PENDING_COUNTER_CLAIM_ISSUED_TASK_DESCRIPTOR
                         .getTaskName(),
                     taskInstance.getTaskName());
        CounterClaimStatusChangeTaskData data = (CounterClaimStatusChangeTaskData) taskInstance.getData();
        assertEquals(counterClaimId, data.getCounterClaimId());
    }

    @Test
    void shouldNotScheduleNotificationOnPostPersistWhenStatusIsNotPendingCounterClaimIssued() {
        CounterClaimEntity entity = new CounterClaimEntity();
        entity.setStatus(CounterClaimState.COUNTER_CLAIM_ISSUED);

        underTest.onPostPersist(entity);

        verify(schedulerClient, never()).scheduleIfNotExists(any());
    }

    @Test
    void shouldDoNothingOnPostUpdateWhenStatusHasNotChanged() {
        CounterClaimEntity entity = new CounterClaimEntity();
        entity.setStatus(CounterClaimState.PENDING_COUNTER_CLAIM_ISSUED);
        entity.setPreviousStatus(CounterClaimState.PENDING_COUNTER_CLAIM_ISSUED);

        underTest.onPostUpdate(entity);

        verify(schedulerClient, never()).scheduleIfNotExists(any());
    }

    @Test
    void shouldSchedulePendingNotificationOnPostUpdateWhenStatusChangesToPending() {
        UUID counterClaimId = UUID.randomUUID();
        CounterClaimEntity entity = CounterClaimEntity.builder()
            .id(counterClaimId)
            .status(CounterClaimState.PENDING_COUNTER_CLAIM_ISSUED)
            .previousStatus(CounterClaimState.COUNTER_CLAIM_ISSUED)
            .build();

        underTest.onPostUpdate(entity);

        ArgumentCaptor<SchedulableInstance<?>> taskInstanceCaptor = ArgumentCaptor.forClass(SchedulableInstance.class);
        verify(schedulerClient).scheduleIfNotExists(taskInstanceCaptor.capture());

        SchedulableInstance<?> schedulableInstance = taskInstanceCaptor.getValue();
        TaskInstance<?> taskInstance = schedulableInstance.getTaskInstance();
        assertEquals(PendingCounterClaimIssuedNotificationTaskComponent.PENDING_COUNTER_CLAIM_ISSUED_TASK_DESCRIPTOR
                         .getTaskName(),
                     taskInstance.getTaskName());
        CounterClaimStatusChangeTaskData data = (CounterClaimStatusChangeTaskData) taskInstance.getData();
        assertEquals(counterClaimId, data.getCounterClaimId());
    }

    @Test
    void shouldScheduleIssuedNotificationOnPostUpdateWhenStatusChangesToIssued() {
        UUID counterClaimId = UUID.randomUUID();
        CounterClaimEntity entity = CounterClaimEntity.builder()
            .id(counterClaimId)
            .status(CounterClaimState.COUNTER_CLAIM_ISSUED)
            .previousStatus(CounterClaimState.PENDING_COUNTER_CLAIM_ISSUED)
            .build();

        underTest.onPostUpdate(entity);

        ArgumentCaptor<SchedulableInstance<?>> taskInstanceCaptor = ArgumentCaptor.forClass(SchedulableInstance.class);
        verify(schedulerClient).scheduleIfNotExists(taskInstanceCaptor.capture());

        SchedulableInstance<?> schedulableInstance = taskInstanceCaptor.getValue();
        TaskInstance<?> taskInstance = schedulableInstance.getTaskInstance();
        assertEquals(CounterClaimIssuedNotificationTaskComponent.COUNTER_CLAIM_ISSUED_TASK_DESCRIPTOR.getTaskName(),
                     taskInstance.getTaskName());
        CounterClaimStatusChangeTaskData data = (CounterClaimStatusChangeTaskData) taskInstance.getData();
        assertEquals(counterClaimId, data.getCounterClaimId());
    }
}
