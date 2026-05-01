package uk.gov.hmcts.reform.pcs.ccd.service.dashboard.task;

import java.util.List;

import org.springframework.stereotype.Component;

import uk.gov.hmcts.reform.pcs.ccd.domain.dashboard.Task;
import uk.gov.hmcts.reform.pcs.ccd.domain.dashboard.TaskGroup;
import uk.gov.hmcts.reform.pcs.ccd.domain.dashboard.TaskGroupId;
import uk.gov.hmcts.reform.pcs.ccd.domain.dashboard.TaskStatus;
import uk.gov.hmcts.reform.pcs.ccd.service.dashboard.DashboardContext;
import uk.gov.hmcts.reform.pcs.ccd.util.ListValueUtils;

@Component
public class ResponseTaskGroupEvaluator implements TaskGroupEvaluator {

    @Override
    public TaskGroupId groupId() {
        return TaskGroupId.RESPONSE;
    }

    @Override
    public TaskGroup evaluate(DashboardContext ctx) {

        TaskStatus respondToClaimStatus = ctx.hasSubmittedResponse()
            ? TaskStatus.COMPLETED
            : ctx.hasDraftResponse()
                ? TaskStatus.IN_PROGRESS
                : TaskStatus.NOT_STARTED;

        TaskStatus viewResponseStatus = ctx.hasSubmittedResponse()
                ? TaskStatus.AVAILABLE
                : TaskStatus.NOT_AVAILABLE;

        return TaskGroup.builder()
            .groupId(groupId())
            .tasks(ListValueUtils.wrapListItems(List.of(
                Task.builder()
                    .templateId("Defendant.RespondToClaim")
                    .status(respondToClaimStatus)
                    .build(),
                Task.builder()
                    .templateId("Defendant.ViewResponse")
                    .status(viewResponseStatus)
                    .build()
            )))
            .build();
    }
}