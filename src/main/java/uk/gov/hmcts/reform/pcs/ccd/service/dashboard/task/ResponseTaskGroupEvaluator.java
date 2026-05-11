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

        boolean hasDraftResponse = ctx.hasDraftResponse();
        boolean hasSubmittedResponse = ctx.hasSubmittedResponse();

        TaskStatus respondToClaimStatus;
        TaskStatus viewResponseStatus;

        // If conflicting state exists, disable both links.
        if (hasSubmittedResponse) {
            respondToClaimStatus = TaskStatus.COMPLETED;
            viewResponseStatus = TaskStatus.AVAILABLE;
        } else if (hasDraftResponse) {
            respondToClaimStatus = TaskStatus.IN_PROGRESS;
            viewResponseStatus = TaskStatus.NOT_AVAILABLE;
        } else {
            respondToClaimStatus = TaskStatus.NOT_STARTED;
            viewResponseStatus = TaskStatus.NOT_AVAILABLE;
        }

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