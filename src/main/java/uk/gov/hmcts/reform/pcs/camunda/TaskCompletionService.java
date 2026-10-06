package uk.gov.hmcts.reform.pcs.camunda;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.reform.pcs.camunda.TaskManagementApi.SearchParameter;
import uk.gov.hmcts.reform.pcs.camunda.TaskManagementApi.Task;
import uk.gov.hmcts.reform.pcs.camunda.TaskManagementApi.TaskSearchRequest;
import uk.gov.hmcts.reform.pcs.ccd.CaseType;
import uk.gov.hmcts.reform.pcs.security.SecurityContextService;
import uk.gov.hmcts.reform.pcs.service.FeatureFlag;
import uk.gov.hmcts.reform.pcs.service.FeatureToggleService;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Completes a task in task management as the user who did its work, as XUI does when an event a task
 * links to is submitted there. Work done in pcs-frontend never reaches XUI, so pcs-api completes the
 * task itself, and the task is complete when the user is back in XUI. The task is found by its order,
 * among the open tasks the user can see on the case.
 */
@Slf4j
@AllArgsConstructor
@Service
public class TaskCompletionService {

    private static final int MAX_TASKS = 50;

    private final TaskManagementApi taskManagementApi;
    private final AuthTokenGenerator authTokenGenerator;
    private final SecurityContextService securityContextService;
    private final FeatureToggleService featureToggleService;

    /**
     * Completes the user's open tasks of a type about one order on the case. Returns whether that
     * worked, rather than failing the work they've done: anything left open is the caller's to close.
     */
    public boolean completeOrderTasks(long caseReference, TaskType taskType, UUID orderId) {
        if (!featureToggleService.isEnabled(FeatureFlag.CASEWORKER_WA)) {
            return true;
        }
        try {
            String user = securityContextService.getCurrentUserAuthToken();
            String service = authTokenGenerator.generate();
            List<Task> tasks = taskManagementApi
                .searchTasks(user, service, 0, MAX_TASKS, search(caseReference, taskType))
                .tasks().stream()
                .filter(task -> orderId.toString().equals(orderId(task)))
                .toList();
            for (Task task : tasks) {
                taskManagementApi.completeTask(user, service, task.id(), Map.of());
            }
            log.info("Completed {} {} tasks for order {} on case {}", tasks.size(), taskType.getId(), orderId,
                     caseReference);
            return !tasks.isEmpty();
        } catch (Exception e) {
            log.warn("Could not complete {} task for order {} on case {}", taskType.getId(), orderId, caseReference, e);
            return false;
        }
    }

    private static TaskSearchRequest search(long caseReference, TaskType taskType) {
        return new TaskSearchRequest(List.of(
            new SearchParameter("jurisdiction", "IN", List.of(CaseType.getJurisdictionId())),
            new SearchParameter("case_id", "IN", List.of(Long.toString(caseReference))),
            new SearchParameter("task_type", "IN", List.of(taskType.getId())),
            new SearchParameter("state", "IN", List.of("UNASSIGNED", "ASSIGNED"))
        ));
    }

    private static String orderId(Task task) {
        return task.additionalProperties() == null ? null : task.additionalProperties().get("orderId");
    }
}
