package uk.gov.hmcts.reform.pcs.ccd.service.counterclaimform;

import com.github.kagkarlsson.scheduler.SchedulerClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.pcs.ccd.model.CounterClaimFormTaskData;
import uk.gov.hmcts.reform.pcs.ccd.task.CounterClaimFormGenerationComponent;

import java.time.Instant;
import java.util.UUID;

@Component
@Slf4j
public class CounterClaimFormScheduler {
    private final SchedulerClient schedulerClient;

    public CounterClaimFormScheduler(SchedulerClient schedulerClient) {
        this.schedulerClient = schedulerClient;
    }

    public void scheduleCounterClaimFormGeneration(UUID counterClaimId) {
        CounterClaimFormTaskData taskData = CounterClaimFormTaskData.builder()
            .counterClaimId(counterClaimId)
            .build();

        boolean scheduled = schedulerClient.scheduleIfNotExists(
            CounterClaimFormGenerationComponent.COUNTER_CLAIM_FORM_TASK_DESCRIPTOR
                .instance(String.valueOf(counterClaimId))
                .data(taskData)
                .scheduledTo(Instant.now())
        );

        if (scheduled) {
            log.info("Scheduled counter claim form generation for counter claim {}", counterClaimId);
        } else {
            log.info("Counter claim form generation already scheduled for counter claim {}, skipping", counterClaimId);
        }
    }
}
