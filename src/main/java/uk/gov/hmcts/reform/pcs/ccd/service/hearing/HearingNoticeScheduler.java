package uk.gov.hmcts.reform.pcs.ccd.service.hearing;

import com.github.kagkarlsson.scheduler.SchedulerClient;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.pcs.ccd.model.HearingNoticeTaskData;
import uk.gov.hmcts.reform.pcs.ccd.task.HearingNoticeGenerationComponent;
import uk.gov.hmcts.reform.pcs.service.FeatureFlag;
import uk.gov.hmcts.reform.pcs.service.FeatureToggleService;

import java.time.Instant;

@Component
public class HearingNoticeScheduler {

    private final SchedulerClient schedulerClient;
    private final FeatureToggleService featureToggleService;

    public HearingNoticeScheduler(SchedulerClient schedulerClient, FeatureToggleService featureToggleService) {
        this.schedulerClient = schedulerClient;
        this.featureToggleService = featureToggleService;
    }

    public void scheduleHearingNoticeGeneration(long caseReference, Integer hearingId) {
        if (!featureToggleService.isEnabled(FeatureFlag.RELEASE_1_DOT_4)) {
            return;
        }
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
