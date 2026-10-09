package uk.gov.hmcts.reform.pcs.client;

import org.awaitility.Awaitility;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import uk.gov.hmcts.reform.pcs.camunda.TaskType;
import uk.gov.hmcts.reform.pcs.model.TaskManagementResponse;
import uk.gov.hmcts.reform.pcs.model.WaTask;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static java.util.Collections.singletonList;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.not;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static uk.gov.hmcts.reform.pcs.auth.ServiceAuthorizationGenerator.generateTestS2SToken;

@Component
public class TaskManagementClient {

    @Autowired
    private TaskMonitorClient taskMonitorClient;

    private final String serviceAuthorisation;
    private final RestClient restClient;

    private static final String BASE_URL = "http://localhost:8087/task/extended-search";

    public TaskManagementClient() {
        this.restClient = RestClient.create(BASE_URL);
        this.serviceAuthorisation = generateTestS2SToken("pcs_api");
    }

    public List<WaTask> waitForTasksOfType(
        long caseId,
        TaskType taskType,
        String authorisation
    ) {
        return Awaitility.await()
            .atMost(Duration.ofSeconds(30))
            .pollInterval(Duration.ofMillis(500))
            .until(() -> getTasksOfTypeForCase(caseId, taskType, authorisation), not(empty()));
    }

    private List<WaTask> getTasksOfTypeForCase(
        long caseId,
        TaskType taskType,
        String authorisation
    ) {
        taskMonitorClient.triggerInitiationJob();

        Map<String, Object> searchParameter = Map.of(
            "key", "caseId",
            "operator", "IN",
            "values", singletonList(caseId)
        );

        Map<String, Object> searchParameter2 = Map.of(
            "key", "task_type",
            "operator", "IN",
            "values", singletonList(taskType.getId())
        );

        Map<String, Set<Map<String, Object>>> requestBody
            = Map.of("search_parameters", Set.of(searchParameter, searchParameter2));

        ResponseEntity<TaskManagementResponse> response = restClient
            .post()
            .contentType(APPLICATION_JSON)
            .header("ServiceAuthorization", serviceAuthorisation)
            .header("Authorization", authorisation)
            .body(requestBody).retrieve().toEntity(TaskManagementResponse.class);

        TaskManagementResponse body = response.getBody();
        return body != null ? body.getTasks() : List.of();
    }
}
