package uk.gov.hmcts.reform.pcs.ccd.service.defenceform;

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
@SuppressWarnings({"unchecked", "rawtypes"})  // Mockito + db-scheduler's SchedulableInstance<T> generics
class DefenceFormSchedulerTest {

    private static final long CASE_REFERENCE = 1234567812345678L;
    private static final UUID RESPONSE_ID = UUID.randomUUID();
    private static final UUID PARTY_ID = UUID.randomUUID();

    @Mock
    private SchedulerClient schedulerClient;

    @InjectMocks
    private DefenceFormScheduler scheduler;

    @Test
    void usesDefendantResponseIdAsInstanceIdForProducerDedup() {
        when(schedulerClient.scheduleIfNotExists((SchedulableInstance<Object>) any())).thenReturn(true);

        scheduler.scheduleDefenceFormGeneration(CASE_REFERENCE, RESPONSE_ID, PARTY_ID);

        ArgumentCaptor<SchedulableInstance> captor = ArgumentCaptor.forClass(SchedulableInstance.class);
        verify(schedulerClient).scheduleIfNotExists(captor.capture());

        // Instance id MUST be the defendant response id so a re-submit can't insert a second row.
        assertThat(captor.getValue().getId()).isEqualTo(RESPONSE_ID.toString());
    }

    @Test
    void rescheduleForSameResponseIsNoOp() {
        when(schedulerClient.scheduleIfNotExists((SchedulableInstance<Object>) any())).thenReturn(false);

        scheduler.scheduleDefenceFormGeneration(CASE_REFERENCE, RESPONSE_ID, PARTY_ID);

        verify(schedulerClient).scheduleIfNotExists((SchedulableInstance<Object>) any());
    }
}
