package uk.gov.hmcts.reform.pcs.ccd.service.claimform;

import com.github.kagkarlsson.scheduler.SchedulerClient;
import com.github.kagkarlsson.scheduler.task.SchedulableInstance;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings({"unchecked", "rawtypes"})  // Mockito + db-scheduler's SchedulableInstance<T> generics
class ClaimFormSchedulerTest {

    private static final long CASE_REFERENCE = 1234567812345678L;

    @Mock
    private SchedulerClient schedulerClient;

    @InjectMocks
    private ClaimFormScheduler scheduler;

    @Test
    void usesCaseReferenceAsInstanceIdForProducerDedup() {
        when(schedulerClient.scheduleIfNotExists((SchedulableInstance<Object>) any())).thenReturn(true);

        scheduler.scheduleClaimFormGeneration(CASE_REFERENCE);

        ArgumentCaptor<SchedulableInstance> captor = ArgumentCaptor.forClass(SchedulableInstance.class);
        verify(schedulerClient).scheduleIfNotExists(captor.capture());

        // Instance id MUST be the case reference (string form) so a re-fired payment callback
        // can't insert a second row for the same case — §3.1 producer-side dedup invariant.
        assertThat(captor.getValue().getId()).isEqualTo(String.valueOf(CASE_REFERENCE));
    }

    @Test
    void rescheduleForSameCaseIsNoOp() {
        when(schedulerClient.scheduleIfNotExists((SchedulableInstance<Object>) any())).thenReturn(false);
        // `scheduleIfNotExists` returning false is the happy-path signal that a row already
        // exists for this case. No exception, no second insertion — just a log line.
        scheduler.scheduleClaimFormGeneration(CASE_REFERENCE);
        verify(schedulerClient).scheduleIfNotExists((SchedulableInstance<Object>) any());
    }
}
