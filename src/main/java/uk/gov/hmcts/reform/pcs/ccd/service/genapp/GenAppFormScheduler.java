package uk.gov.hmcts.reform.pcs.ccd.service.genapp;

import com.github.kagkarlsson.scheduler.SchedulerClient;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.pcs.ccd.model.GenAppDocumentTaskData;
import uk.gov.hmcts.reform.pcs.ccd.task.GenAppDocumentGenerationComponent;

import java.time.Instant;
import java.util.UUID;

/**
 * Schedules the gen-app-document-generation task once the application fee has been paid.
 *
 * <p>The gen app id is used as the db-scheduler instance id, so {@code scheduleIfNotExists}
 * only inserts on the first call; a re-fired payment callback for the same application is a
 * no-op.</p>
 */
@Component
public class GenAppFormScheduler {

    private final SchedulerClient schedulerClient;

    public GenAppFormScheduler(SchedulerClient schedulerClient) {
        this.schedulerClient = schedulerClient;
    }

    public void scheduleGenAppDocumentGeneration(UUID genAppId) {
        GenAppDocumentTaskData taskData = GenAppDocumentTaskData.builder()
            .genAppId(genAppId)
            .build();

        schedulerClient.scheduleIfNotExists(
            GenAppDocumentGenerationComponent.GEN_APP_DOCUMENT_TASK_DESCRIPTOR
                .instance(String.valueOf(genAppId))
                .data(taskData)
                .scheduledTo(Instant.now())
        );
    }
}
