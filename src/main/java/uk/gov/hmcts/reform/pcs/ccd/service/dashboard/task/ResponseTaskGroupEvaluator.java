package uk.gov.hmcts.reform.pcs.ccd.service.dashboard.task;

import java.util.List;

import org.springframework.stereotype.Component;

import uk.gov.hmcts.reform.pcs.ccd.domain.dashboard.Task;
import uk.gov.hmcts.reform.pcs.ccd.domain.dashboard.TaskGroup;
import uk.gov.hmcts.reform.pcs.ccd.domain.dashboard.TaskGroupId;
import uk.gov.hmcts.reform.pcs.ccd.domain.dashboard.TaskStatus;
import uk.gov.hmcts.reform.pcs.ccd.service.dashboard.DashboardContext;
import uk.gov.hmcts.reform.pcs.ccd.util.ListValueUtils;

import static uk.gov.hmcts.reform.pcs.ccd.domain.dashboard.DashboardTaskTemplateIds.RESPOND_TO_CLAIM;
import static uk.gov.hmcts.reform.pcs.ccd.domain.dashboard.DashboardTaskTemplateIds.REVIEW_RESPONSE;
import static uk.gov.hmcts.reform.pcs.ccd.domain.dashboard.DashboardTaskTemplateIds.SUBMIT_RESPONSE;

@Component
public class ResponseTaskGroupEvaluator implements TaskGroupEvaluator {

    @Override
    public TaskGroupId groupId() {
        return TaskGroupId.RESPONSE;
    }

    @Override
    public TaskGroup evaluate(DashboardContext ctx) {
        return TaskGroup.builder()
            .groupId(TaskGroupId.RESPONSE)
            .tasks(ListValueUtils.wrapListItems(List.of(
                Task.builder()
                    .templateId(RESPOND_TO_CLAIM)
                    .status(TaskStatus.NOT_STARTED)
                    .build(),
                Task.builder()
                    .templateId(REVIEW_RESPONSE)
                    .status(TaskStatus.IN_PROGRESS)
                    .build(),
                Task.builder()
                    .templateId(SUBMIT_RESPONSE)
                    .status(TaskStatus.COMPLETED)
                    .build()
            )))
            .build();
    }
}
