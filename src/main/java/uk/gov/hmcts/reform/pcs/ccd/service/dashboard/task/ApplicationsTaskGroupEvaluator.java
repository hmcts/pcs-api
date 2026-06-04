package uk.gov.hmcts.reform.pcs.ccd.service.dashboard.task;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Component;

import uk.gov.hmcts.reform.pcs.ccd.domain.dashboard.Task;
import uk.gov.hmcts.reform.pcs.ccd.domain.dashboard.TaskGroup;
import uk.gov.hmcts.reform.pcs.ccd.domain.dashboard.TaskGroupId;
import uk.gov.hmcts.reform.pcs.ccd.domain.dashboard.TaskStatus;
import uk.gov.hmcts.reform.pcs.ccd.service.dashboard.DashboardContext;
import uk.gov.hmcts.reform.pcs.ccd.util.ListValueUtils;

import static uk.gov.hmcts.reform.pcs.ccd.domain.dashboard.DashboardTaskTemplateIds.MAKE_GENERAL_APPLICATION;
import static uk.gov.hmcts.reform.pcs.ccd.domain.dashboard.DashboardTaskTemplateIds.VIEW_ALL_APPLICATIONS;

@Component
public class ApplicationsTaskGroupEvaluator implements TaskGroupEvaluator {

    @Override
    public TaskGroupId groupId() {
        return TaskGroupId.APPLICATIONS;
    }

    @Override
    public TaskGroup evaluate(DashboardContext ctx) {
        return TaskGroup.builder()
            .groupId(TaskGroupId.APPLICATIONS)
            .tasks(ListValueUtils.wrapListItems(List.of(
                Task.builder()
                    .templateId(MAKE_GENERAL_APPLICATION)
                    .status(TaskStatus.AVAILABLE)
                    .build(),
                Task.builder()
                    .templateId(VIEW_ALL_APPLICATIONS)
                    .status(hasRaisedGeneralApplications(ctx) ? TaskStatus.AVAILABLE : TaskStatus.NOT_AVAILABLE)
                    .build()
            )))
            .build();
    }

    private boolean hasRaisedGeneralApplications(DashboardContext ctx) {
        if (ctx == null || ctx.caseEntity() == null || ctx.caseEntity().getGenApps() == null) {
            return false;
        }
        UUID viewerIdamId = ctx.defendant() != null ? ctx.defendant().getIdamId() : null;
        return ctx.caseEntity().getGenApps().stream()
            .anyMatch(genApp -> ctx.isVisibleToUser(genApp, viewerIdamId));
    }
}
