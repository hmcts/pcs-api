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
import uk.gov.hmcts.reform.pcs.ccd.domain.respondpossessionclaim.DefendantResponseStatus;
import uk.gov.hmcts.reform.pcs.ccd.entity.respondpossessionclaim.DefendantResponseEntity;
import uk.gov.hmcts.reform.pcs.ccd.model.DefendantResponseStatusChangeTaskData;
import uk.gov.hmcts.reform.pcs.ccd.task.DefendantResponseSubmittedNotificationTaskComponent;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DefendantResponseEntityListenerTest {

    @Mock
    private SchedulerClient schedulerClient;

    @InjectMocks
    private DefendantResponseEntityListener underTest;

    @Test
    void shouldSetPreviousStatusOnPostLoad() {
        DefendantResponseEntity entity = new DefendantResponseEntity();
        entity.setStatus(DefendantResponseStatus.CREATED);

        underTest.onPostLoad(entity);

        assertEquals(DefendantResponseStatus.CREATED, entity.getPreviousStatus());
    }

    @Test
    void shouldScheduleNotificationOnPostPersistWhenStatusIsSubmitted() {
        UUID defendantResponseId = UUID.randomUUID();
        DefendantResponseEntity entity = mock(DefendantResponseEntity.class);
        when(entity.getStatus()).thenReturn(DefendantResponseStatus.SUBMITTED);
        when(entity.getId()).thenReturn(defendantResponseId);

        underTest.onPostPersist(entity);

        ArgumentCaptor<SchedulableInstance<?>> taskInstanceCaptor = ArgumentCaptor.forClass(SchedulableInstance.class);
        verify(schedulerClient).scheduleIfNotExists(taskInstanceCaptor.capture());

        SchedulableInstance<?> schedulableInstance = taskInstanceCaptor.getValue();
        TaskInstance<?> taskInstance = schedulableInstance.getTaskInstance();
        assertEquals(DefendantResponseSubmittedNotificationTaskComponent.DEFENDANT_RESPONSE_SUBMITTED_TASK_DESCRIPTOR
                         .getTaskName(),
                     taskInstance.getTaskName());
        DefendantResponseStatusChangeTaskData data = (DefendantResponseStatusChangeTaskData) taskInstance.getData();
        assertEquals(defendantResponseId, data.getDefendantResponseId());
    }

    @Test
    void shouldNotScheduleNotificationOnPostPersistWhenStatusIsNotSubmitted() {
        DefendantResponseEntity entity = mock(DefendantResponseEntity.class);
        when(entity.getStatus()).thenReturn(DefendantResponseStatus.CREATED);

        underTest.onPostPersist(entity);

        verify(schedulerClient, never()).scheduleIfNotExists(any());
    }

    @Test
    void shouldDoNothingOnPostUpdateWhenStatusHasNotChanged() {
        DefendantResponseEntity entity = mock(DefendantResponseEntity.class);
        when(entity.getStatus()).thenReturn(DefendantResponseStatus.CREATED);
        when(entity.getPreviousStatus()).thenReturn(DefendantResponseStatus.CREATED);

        underTest.onPostUpdate(entity);

        verify(schedulerClient, never()).scheduleIfNotExists(any());
    }

    @Test
    void shouldScheduleNotificationOnPostUpdateWhenStatusChangesToSubmitted() {
        UUID defendantResponseId = UUID.randomUUID();
        DefendantResponseEntity entity = mock(DefendantResponseEntity.class);
        when(entity.getStatus()).thenReturn(DefendantResponseStatus.SUBMITTED);
        when(entity.getPreviousStatus()).thenReturn(DefendantResponseStatus.CREATED);
        when(entity.getId()).thenReturn(defendantResponseId);

        underTest.onPostUpdate(entity);

        ArgumentCaptor<SchedulableInstance<?>> taskInstanceCaptor = ArgumentCaptor.forClass(SchedulableInstance.class);
        verify(schedulerClient).scheduleIfNotExists(taskInstanceCaptor.capture());

        SchedulableInstance<?> schedulableInstance = taskInstanceCaptor.getValue();
        TaskInstance<?> taskInstance = schedulableInstance.getTaskInstance();
        assertEquals(DefendantResponseSubmittedNotificationTaskComponent.DEFENDANT_RESPONSE_SUBMITTED_TASK_DESCRIPTOR
                .getTaskName(),
                     taskInstance.getTaskName());
        DefendantResponseStatusChangeTaskData data = (DefendantResponseStatusChangeTaskData) taskInstance.getData();
        assertEquals(defendantResponseId, data.getDefendantResponseId());
    }

    @Test
    void shouldNotScheduleNotificationOnPostUpdateWhenStatusChangesToSomethingElse() {
        DefendantResponseEntity entity = mock(DefendantResponseEntity.class);
        when(entity.getStatus()).thenReturn(DefendantResponseStatus.CREATED);
        when(entity.getPreviousStatus()).thenReturn(null);

        underTest.onPostUpdate(entity);

        verify(schedulerClient, never()).scheduleIfNotExists(any());
    }
}
