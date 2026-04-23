package uk.gov.hmcts.reform.pcs.ccd.service.dashboard.task;

import java.util.Comparator;
import java.util.List;

import org.springframework.stereotype.Component;

import uk.gov.hmcts.ccd.sdk.type.ListValue;
import uk.gov.hmcts.reform.pcs.ccd.domain.dashboard.TaskGroup;
import uk.gov.hmcts.reform.pcs.ccd.service.dashboard.DashboardContext;
import uk.gov.hmcts.reform.pcs.ccd.util.ListValueUtils;

@Component
public class TaskGroupAssembler {
  private final List<TaskGroupEvaluator> evaluators;
  public TaskGroupAssembler(List<TaskGroupEvaluator> evaluators) {
      this.evaluators = evaluators.stream()
          .sorted(Comparator.comparingInt(e -> e.groupId().ordinal()))
          .toList();
  }
  public List<ListValue<TaskGroup>> assemble(DashboardContext ctx) {
      List<TaskGroup> taskGroups = evaluators.stream()
          .map(e -> e.evaluate(ctx))
          .toList();
      return ListValueUtils.wrapListItems(taskGroups);
  }
}