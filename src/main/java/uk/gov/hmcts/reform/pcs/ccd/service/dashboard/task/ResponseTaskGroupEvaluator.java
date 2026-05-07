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

        // if conflicting state exists, disable links and omit tags.
        boolean edgeCase = ctx.hasDraftResponse() && ctx.hasSubmittedResponse();

        TaskStatus respondToClaimStatus = edgeCase
            ? TaskStatus.NOT_AVAILABLE
            : ctx.hasSubmittedResponse()
                ? TaskStatus.COMPLETED
                : ctx.hasDraftResponse()
                    ? TaskStatus.IN_PROGRESS
                    : TaskStatus.NOT_STARTED;

        TaskStatus viewResponseStatus = !edgeCase && ctx.hasSubmittedResponse()
                ? TaskStatus.AVAILABLE
                : TaskStatus.NOT_AVAILABLE;

        return TaskGroup.builder()
            .groupId(groupId())
            .tasks(ListValueUtils.wrapListItems(List.of(
                Task.builder()
                    .templateId("RespondToClaim")
                    .status(respondToClaimStatus)
                    .build(),
                Task.builder()
                    .templateId("ViewResponse")
                    .status(viewResponseStatus)
                    .build()
            )))
            .build();
    }
}