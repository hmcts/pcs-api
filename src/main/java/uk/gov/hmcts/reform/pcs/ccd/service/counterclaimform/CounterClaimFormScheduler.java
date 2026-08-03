package uk.gov.hmcts.reform.pcs.ccd.service.counterclaimform;

import com.github.kagkarlsson.scheduler.SchedulerClient;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.pcs.ccd.model.CounterClaimFormTaskData;
import uk.gov.hmcts.reform.pcs.ccd.task.CounterClaimFormGenerationComponent;

import java.time.Instant;
import java.util.UUID;

@Component
@AllArgsConstructor
public class CounterClaimFormScheduler {
    private final SchedulerClient schedulerClient;

    public void scheduleCounterClaimFormGeneration(UUID counterClaimId) {
        CounterClaimFormTaskData taskData = CounterClaimFormTaskData.builder()
            .counterClaimId(counterClaimId)
            .build();

        schedulerClient.scheduleIfNotExists(
            CounterClaimFormGenerationComponent.COUNTER_CLAIM_FORM_TASK_DESCRIPTOR
                .instance(String.valueOf(counterClaimId))
                .data(taskData)
                .scheduledTo(Instant.now())
        );
    }
}
