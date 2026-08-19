package uk.gov.hmcts.reform.pcs.ccd.service.dashboard.task;

import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.pcs.ccd.domain.dashboard.Task;
import uk.gov.hmcts.reform.pcs.ccd.domain.dashboard.TaskGroup;
import uk.gov.hmcts.reform.pcs.ccd.domain.dashboard.TaskGroupId;
import uk.gov.hmcts.reform.pcs.ccd.domain.dashboard.TaskStatus;
import uk.gov.hmcts.reform.pcs.ccd.service.UserRoles;
import uk.gov.hmcts.reform.pcs.ccd.service.UserRoleService;
import uk.gov.hmcts.reform.pcs.ccd.service.dashboard.DashboardContext;
import uk.gov.hmcts.reform.pcs.ccd.service.genapp.GenAppVisibilityService;
import uk.gov.hmcts.reform.pcs.ccd.util.ListValueUtils;

import java.util.List;

import static uk.gov.hmcts.reform.pcs.ccd.domain.dashboard.DashboardTaskTemplateIds.MAKE_GENERAL_APPLICATION;
import static uk.gov.hmcts.reform.pcs.ccd.domain.dashboard.DashboardTaskTemplateIds.VIEW_ALL_APPLICATIONS;

@Component
public class ApplicationsTaskGroupEvaluator implements TaskGroupEvaluator {

    private final UserRoleService userRoleService;
    private final GenAppVisibilityService genAppVisibilityService;

    public ApplicationsTaskGroupEvaluator(
        UserRoleService userRoleService,
        GenAppVisibilityService genAppVisibilityService
    ) {
        this.userRoleService = userRoleService;
        this.genAppVisibilityService = genAppVisibilityService;
    }

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
        if (ctx == null || ctx.caseEntity() == null || ctx.caseEntity().getGenApps() == null
            || ctx.caseEntity().getGenApps().isEmpty()) {
            return false;
        }

        UserRoles userRoles =
            userRoleService.getCurrentUserCaseRoles(ctx.caseReference());

        return !genAppVisibilityService.getVisibleGenAppsToUser(
            ctx.caseEntity().getGenApps(),
            userRoles.userId(),
            userRoles.roles()
        ).isEmpty();
    }
}
