package uk.gov.hmcts.reform.pcs.notify.listener;

import com.github.kagkarlsson.scheduler.SchedulerClient;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.PostUpdate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.pcs.ccd.domain.respondpossessionclaim.DefendantResponseStatus;
import uk.gov.hmcts.reform.pcs.ccd.entity.respondpossessionclaim.DefendantResponseEntity;
import uk.gov.hmcts.reform.pcs.ccd.model.DefendantResponseStatusChangeTaskData;
import uk.gov.hmcts.reform.pcs.ccd.task.DefendantResponseSubmittedNotificationTaskComponent;

import java.time.Instant;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class DefendantResponseEntityListener {

    private final SchedulerClient schedulerClient;

    @PostLoad
    public void onPostLoad(DefendantResponseEntity entity) {
        entity.setPreviousStatus(entity.getStatus());
    }

    @PostPersist
    public void onPostPersist(DefendantResponseEntity entity) {
        if (entity.getStatus() == DefendantResponseStatus.SUBMITTED) {
            scheduleDefendantResponseSubmittedNotification(entity);
        }
    }

    @PostUpdate
    public void onPostUpdate(DefendantResponseEntity entity) {
        if (entity.getStatus() == entity.getPreviousStatus()) {
            return;
        }

        if (entity.getStatus() == DefendantResponseStatus.SUBMITTED) {
            scheduleDefendantResponseSubmittedNotification(entity);
        }
    }

    private void scheduleDefendantResponseSubmittedNotification(DefendantResponseEntity defendantResponse) {
        String taskId = UUID.randomUUID().toString();
        UUID defendantResponseId = defendantResponse.getId();
        log.info("Scheduling defendant response submitted notification for: {}, with task id: {}",
                 defendantResponseId,
                 taskId);

        schedulerClient.scheduleIfNotExists(
            DefendantResponseSubmittedNotificationTaskComponent.DEFENDANT_RESPONSE_SUBMITTED_TASK_DESCRIPTOR
                .instance(taskId)
                .data(DefendantResponseStatusChangeTaskData.builder()
                          .defendantResponseId(defendantResponseId)
                          .build())
                .scheduledTo(Instant.now())
        );
    }
}
