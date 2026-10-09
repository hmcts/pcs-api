package uk.gov.hmcts.reform.pcs.camunda;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.reform.pcs.security.SecurityContextService;
import uk.gov.hmcts.reform.pcs.service.FeatureFlag;
import uk.gov.hmcts.reform.pcs.service.FeatureToggleService;

import java.util.Map;

/**
 * Completes a Work Allocation task in task management as the user who did its work, by the id XUI put
 * in the task's link, as XUI does when an event a task links to is submitted there. Work done in
 * pcs-frontend never reaches XUI, so pcs-api completes the task itself once the event has committed,
 * and the task is gone from the user's list when they are back in XUI.
 */
@Slf4j
@AllArgsConstructor
@Service
public class TaskCompletionService {

    private final TaskManagementApi taskManagementApi;
    private final AuthTokenGenerator authTokenGenerator;
    private final SecurityContextService securityContextService;
    private final FeatureToggleService featureToggleService;

    /** Returns the task's completion, for the event to run as the user once it has committed. */
    public Runnable complete(String taskId) {
        if (!featureToggleService.isEnabled(FeatureFlag.CASEWORKER_WA) || taskId == null) {
            return () -> { };
        }
        String user = securityContextService.getCurrentUserAuthToken();
        return () -> {
            try {
                taskManagementApi.completeTask(user, authTokenGenerator.generate(), taskId, Map.of());
                log.info("Completed task {}", taskId);
            } catch (RuntimeException e) {
                log.warn("Could not complete task {}", taskId, e);
            }
        };
    }
}
