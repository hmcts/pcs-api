package uk.gov.hmcts.reform.pcs.ccd.service.genapp;

import com.github.kagkarlsson.scheduler.SchedulerClient;
import com.github.kagkarlsson.scheduler.task.SchedulableInstance;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings({"unchecked", "rawtypes"})
class GenAppFormSchedulerTest {

    private static final UUID GEN_APP_ID = UUID.randomUUID();

    @Mock
    private SchedulerClient schedulerClient;

    @InjectMocks
    private GenAppFormScheduler scheduler;

    @Test
    void usesGenAppIdAsInstanceIdForProducerDedup() {
        when(schedulerClient.scheduleIfNotExists(any())).thenReturn(true);

        scheduler.scheduleGenAppDocumentGeneration(GEN_APP_ID);

        ArgumentCaptor<SchedulableInstance> captor = ArgumentCaptor.forClass(SchedulableInstance.class);
        verify(schedulerClient).scheduleIfNotExists(captor.capture());

        assertThat(captor.getValue().getId()).isEqualTo(GEN_APP_ID.toString());
    }

    @Test
    void rescheduleForSameGenAppIsNoOp() {
        when(schedulerClient.scheduleIfNotExists(any())).thenReturn(false);

        scheduler.scheduleGenAppDocumentGeneration(GEN_APP_ID);

        verify(schedulerClient).scheduleIfNotExists(any());
    }
}
