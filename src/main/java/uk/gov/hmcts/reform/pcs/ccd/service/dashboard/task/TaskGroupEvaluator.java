package uk.gov.hmcts.reform.pcs.ccd.service.dashboard.task;

import uk.gov.hmcts.reform.pcs.ccd.domain.dashboard.TaskGroup;
import uk.gov.hmcts.reform.pcs.ccd.domain.dashboard.TaskGroupId;
import uk.gov.hmcts.reform.pcs.ccd.service.dashboard.DashboardContext;

public interface TaskGroupEvaluator {
    TaskGroupId groupId();
    
    TaskGroup evaluate(DashboardContext ctx);
}
