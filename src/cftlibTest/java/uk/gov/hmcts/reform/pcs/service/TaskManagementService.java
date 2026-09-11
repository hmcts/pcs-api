package uk.gov.hmcts.reform.pcs.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import uk.gov.hmcts.reform.pcs.model.TaskManagementResponse;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static java.util.Collections.singletonList;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static uk.gov.hmcts.reform.pcs.auth.ServiceAuthorizationGenerator.generateTestS2SToken;

@Component
public class TaskManagementService {

    @Autowired
    private TaskMonitorService taskMonitorService;

    private final String serviceAuthorisation;
    private final RestClient restClient;

    private static final String BASE_URL = "http://localhost:8087/task/extended-search";

    public TaskManagementService() {
        this.restClient = RestClient.create(BASE_URL);
        this.serviceAuthorisation = generateTestS2SToken("pcs_api");
    }

    public ResponseEntity<TaskManagementResponse> search(
        Long caseId,
        List<String> expectedTaskList,
        String authorisation
    ) throws InterruptedException {

        // Wait 5 seconds to allow time for task to be created in Camunda
        Thread.sleep(5000);

        taskMonitorService.triggerInitiationJob();

        Map<String, Object> searchParameter = Map.of(
            "key", "caseId",
            "operator", "IN",
            "values", singletonList(caseId)
        );

        Map<String, Object> searchParameter2 = Map.of(
            "key", "task_type",
            "operator", "IN",
            "values", expectedTaskList
        );

        Map<String, Set<Map<String, Object>>> requestBody
            = Map.of("search_parameters", Set.of(searchParameter, searchParameter2));

        // Wait 35 seconds to allow time for task to be initialised in WA
        Thread.sleep(35000);

        return restClient
            .post()
            .contentType(APPLICATION_JSON)
            .header("ServiceAuthorization", serviceAuthorisation)
            .header("Authorization", authorisation)
            .body(requestBody).retrieve().toEntity(TaskManagementResponse.class);
    }
}
