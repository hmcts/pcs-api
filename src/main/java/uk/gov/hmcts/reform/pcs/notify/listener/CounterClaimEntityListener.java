package uk.gov.hmcts.reform.pcs.notify.listener;

import com.github.kagkarlsson.scheduler.SchedulerClient;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.PostUpdate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.pcs.ccd.domain.respondpossessionclaim.CounterClaimStatus;
import uk.gov.hmcts.reform.pcs.ccd.entity.respondpossessionclaim.CounterClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.model.CounterClaimStatusChangeTaskData;
import uk.gov.hmcts.reform.pcs.ccd.task.CounterClaimIssuedNotificationTaskComponent;
import uk.gov.hmcts.reform.pcs.ccd.task.PendingCounterClaimIssuedNotificationTaskComponent;

import java.time.Instant;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class CounterClaimEntityListener {

    private final SchedulerClient schedulerClient;

    @PostLoad
    public void onPostLoad(CounterClaimEntity entity) {
        entity.setPreviousStatus(entity.getStatus());
    }

    @PostPersist
    public void onPostPersist(CounterClaimEntity entity) {
        if (entity.getStatus() == CounterClaimStatus.PENDING_COUNTER_CLAIM_ISSUED) {
            schedulePendingCounterClaimIssuedNotification(entity);
        }
    }

    @PostUpdate
    public void onPostUpdate(CounterClaimEntity entity) {
        if (entity.getStatus() == entity.getPreviousStatus()) {
            return;
        }

        switch (entity.getStatus()) {
            case PENDING_COUNTER_CLAIM_ISSUED -> schedulePendingCounterClaimIssuedNotification(entity);
            case COUNTER_CLAIM_ISSUED -> scheduleCounterClaimIssuedNotification(entity);
        }
    }

    private void schedulePendingCounterClaimIssuedNotification(CounterClaimEntity entity) {
        String taskId = UUID.randomUUID().toString();
        UUID counterClaimId = entity.getId();
        log.info("Scheduling pending counter claim issued notification for: {}, with task id: {}",
                 counterClaimId,
                 taskId);

        schedulerClient.scheduleIfNotExists(
            PendingCounterClaimIssuedNotificationTaskComponent.PENDING_COUNTER_CLAIM_ISSUED_TASK_DESCRIPTOR
                .instance(taskId)
                .data(CounterClaimStatusChangeTaskData.builder()
                          .counterClaimId(counterClaimId)
                          .build())
                .scheduledTo(Instant.now())
        );
    }

    private void scheduleCounterClaimIssuedNotification(CounterClaimEntity entity) {
        String taskId = UUID.randomUUID().toString();
        UUID counterClaimId = entity.getId();
        log.info("Scheduling counter claim issued notification for: {}, with task id: {}", counterClaimId, taskId);

        schedulerClient.scheduleIfNotExists(
            CounterClaimIssuedNotificationTaskComponent.COUNTER_CLAIM_ISSUED_TASK_DESCRIPTOR
                .instance(taskId)
                .data(CounterClaimStatusChangeTaskData.builder()
                          .counterClaimId(counterClaimId)
                          .build())
                .scheduledTo(Instant.now())
        );
    }
}
