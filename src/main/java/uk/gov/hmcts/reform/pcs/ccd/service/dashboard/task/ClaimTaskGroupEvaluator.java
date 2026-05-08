package uk.gov.hmcts.reform.pcs.ccd.service.dashboard.task;

import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.pcs.ccd.domain.dashboard.Task;
import uk.gov.hmcts.reform.pcs.ccd.domain.dashboard.TaskGroup;
import uk.gov.hmcts.reform.pcs.ccd.domain.dashboard.TaskGroupId;
import uk.gov.hmcts.reform.pcs.ccd.domain.dashboard.TaskStatus;
import uk.gov.hmcts.reform.pcs.ccd.service.dashboard.DashboardContext;
import uk.gov.hmcts.reform.pcs.ccd.util.ListValueUtils;

import java.util.List;

import static uk.gov.hmcts.reform.pcs.ccd.domain.dashboard.DashboardTaskTemplateIds.VIEW_CLAIM;

@Component
public class ClaimTaskGroupEvaluator implements TaskGroupEvaluator {

    @Override
    public TaskGroupId groupId() {
        return TaskGroupId.CLAIM;
    }

    @Override
    public TaskGroup evaluate(DashboardContext ctx) {
        return TaskGroup.builder()
            .groupId(groupId()) 
            .tasks(ListValueUtils.wrapListItems(List.of(
                Task.builder()
                    .templateId(VIEW_CLAIM)
                    .status(TaskStatus.AVAILABLE)
                    .build()
            )))
            .build();
    }
}
