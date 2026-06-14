package uk.gov.hmcts.reform.pcs.ccd.service.defenceform;

import com.github.kagkarlsson.scheduler.SchedulerClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.pcs.ccd.model.DefenceFormTaskData;
import uk.gov.hmcts.reform.pcs.ccd.task.DefenceFormGenerationComponent;

import java.time.Instant;
import java.util.UUID;

/**
 * Schedules the defence-form-generation task once a defendant has submitted their response.
 *
 * <p>The defendant response id is used as the db-scheduler instance id, so
 * {@code scheduleIfNotExists} only inserts on the first call; a re-submit for the same response is a
 * no-op.</p>
 */
@Component
@Slf4j
public class DefenceFormScheduler {

    private final SchedulerClient schedulerClient;

    public DefenceFormScheduler(SchedulerClient schedulerClient) {
        this.schedulerClient = schedulerClient;
    }

    public void scheduleDefenceFormGeneration(long caseReference, UUID defendantResponseId, UUID defendantPartyId) {
        DefenceFormTaskData taskData = DefenceFormTaskData.builder()
            .caseReference(String.valueOf(caseReference))
            .defendantResponseId(defendantResponseId)
            .defendantPartyId(defendantPartyId)
            .build();

        boolean scheduled = schedulerClient.scheduleIfNotExists(
            DefenceFormGenerationComponent.DEFENCE_FORM_TASK_DESCRIPTOR
                .instance(String.valueOf(defendantResponseId))
                .data(taskData)
                .scheduledTo(Instant.now())
        );

        if (scheduled) {
            log.info("Scheduled defence form generation for defendant response {}", defendantResponseId);
        } else {
            log.info("Defence form generation already scheduled for defendant response {}, skipping",
                     defendantResponseId);
        }
    }
}
