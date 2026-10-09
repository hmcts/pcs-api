package uk.gov.hmcts.reform.pcs.ccd.service.hearing;

import com.github.kagkarlsson.scheduler.SchedulerClient;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.pcs.ccd.model.HearingNoticeTaskData;
import uk.gov.hmcts.reform.pcs.ccd.task.HearingNoticeGenerationComponent;

import java.time.Instant;

@Component
public class HearingNoticeScheduler {

    private final SchedulerClient schedulerClient;

    public HearingNoticeScheduler(SchedulerClient schedulerClient) {
        this.schedulerClient = schedulerClient;
    }

    public void scheduleHearingNoticeGeneration(long caseReference, Integer hearingId) {
        HearingNoticeTaskData taskData = HearingNoticeTaskData.builder()
            .caseReference(caseReference)
            .hearingId(hearingId)
            .build();

        schedulerClient.scheduleIfNotExists(
            HearingNoticeGenerationComponent.HEARING_NOTICE_TASK_DESCRIPTOR
                .instance(String.valueOf(hearingId))
                .data(taskData)
                .scheduledTo(Instant.now())
        );
    }
}
