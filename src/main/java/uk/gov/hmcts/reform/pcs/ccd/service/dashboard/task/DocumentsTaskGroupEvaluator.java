package uk.gov.hmcts.reform.pcs.ccd.service.dashboard.task;

import java.util.List;

import org.springframework.stereotype.Component;

import uk.gov.hmcts.reform.pcs.ccd.domain.dashboard.Task;
import uk.gov.hmcts.reform.pcs.ccd.domain.dashboard.TaskGroup;
import uk.gov.hmcts.reform.pcs.ccd.domain.dashboard.TaskGroupId;
import uk.gov.hmcts.reform.pcs.ccd.domain.dashboard.TaskStatus;
import uk.gov.hmcts.reform.pcs.ccd.service.dashboard.DashboardContext;
import uk.gov.hmcts.reform.pcs.ccd.util.ListValueUtils;

import static uk.gov.hmcts.reform.pcs.ccd.domain.dashboard.DashboardTaskTemplateIds.UPLOAD_DOCUMENTS;
import static uk.gov.hmcts.reform.pcs.ccd.domain.dashboard.DashboardTaskTemplateIds.VIEW_ALL_DOCUMENTS;

@Component
public class DocumentsTaskGroupEvaluator implements TaskGroupEvaluator {

    @Override
    public TaskGroupId groupId() {
        return TaskGroupId.DOCUMENTS;
    }

    @Override
    public TaskGroup evaluate(DashboardContext ctx) {
        return TaskGroup.builder()
            .groupId(groupId())
            .tasks(ListValueUtils.wrapListItems(List.of(
                Task.builder()
                    .templateId(UPLOAD_DOCUMENTS)
                    .status(TaskStatus.AVAILABLE)
                    .build(),
                Task.builder()
                    .templateId(VIEW_ALL_DOCUMENTS)
                    .status(TaskStatus.AVAILABLE)
                    .build()
            )))
            .build();
    }
}
