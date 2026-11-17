package uk.gov.hmcts.reform.pcs.ccd.service.writofpossession;

import com.github.kagkarlsson.scheduler.SchedulerClient;
import com.github.kagkarlsson.scheduler.task.SchedulableInstance;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.pcs.ccd.model.EnforcementDocumentTaskData;
import uk.gov.hmcts.reform.pcs.ccd.task.WritOfPossessionGenerationComponent;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings({"unchecked", "rawtypes"})
class WritOfPossessionSchedulerTest {

    private static final long CASE_REFERENCE = 1234567812345678L;
    private static final UUID ENFORCEMENT_ORDER_ID = UUID.randomUUID();

    @Mock
    private SchedulerClient schedulerClient;

    @InjectMocks
    private WritOfPossessionScheduler scheduler;

    @Test
    void shouldUseTheEnforcementOrderIdAsTheTaskId() {
        when(schedulerClient.scheduleIfNotExists(any())).thenReturn(true);

        scheduler.scheduleWritOfPossessionGeneration(CASE_REFERENCE, ENFORCEMENT_ORDER_ID);

        ArgumentCaptor<SchedulableInstance> captor = ArgumentCaptor.forClass(SchedulableInstance.class);
        verify(schedulerClient).scheduleIfNotExists(captor.capture());

        assertThat(captor.getValue().getTaskName())
            .isEqualTo(WritOfPossessionGenerationComponent.WRIT_OF_POSSESSION_TASK_DESCRIPTOR.getTaskName());
        assertThat(captor.getValue().getId()).isEqualTo(ENFORCEMENT_ORDER_ID.toString());
    }

    @Test
    void shouldPassTheCaseReferenceAndEnforcementOrderIdToTheTask() {
        when(schedulerClient.scheduleIfNotExists(any())).thenReturn(true);

        scheduler.scheduleWritOfPossessionGeneration(CASE_REFERENCE, ENFORCEMENT_ORDER_ID);

        ArgumentCaptor<SchedulableInstance> captor = ArgumentCaptor.forClass(SchedulableInstance.class);
        verify(schedulerClient).scheduleIfNotExists(captor.capture());

        EnforcementDocumentTaskData taskData =
            (EnforcementDocumentTaskData) captor.getValue().getTaskInstance().getData();
        assertThat(taskData.getCaseReference()).isEqualTo(CASE_REFERENCE);
        assertThat(taskData.getEnforcementOrderId()).isEqualTo(ENFORCEMENT_ORDER_ID);
    }

    @Test
    void shouldDoNothingWhenTheSameEnforcementOrderIsScheduledAgain() {
        when(schedulerClient.scheduleIfNotExists(any())).thenReturn(false);

        scheduler.scheduleWritOfPossessionGeneration(CASE_REFERENCE, ENFORCEMENT_ORDER_ID);

        verify(schedulerClient).scheduleIfNotExists(any());
    }
}
