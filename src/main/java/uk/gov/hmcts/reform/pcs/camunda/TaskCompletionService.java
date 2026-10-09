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
 * Completes a Work Allocation task as the user who did its work. XUI does this for events submitted
 * there, but work done in pcs-frontend never reaches XUI.
 */
@Slf4j
@AllArgsConstructor
@Service
public class TaskCompletionService {

    private final TaskManagementApi taskManagementApi;
    private final AuthTokenGenerator authTokenGenerator;
    private final SecurityContextService securityContextService;
    private final FeatureToggleService featureToggleService;

    /** The completion for the event to run once it has committed, as the current user. */
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
