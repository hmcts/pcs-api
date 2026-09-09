package uk.gov.hmcts.reform.pcs.service;

import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Map;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static uk.gov.hmcts.reform.pcs.auth.ServiceAuthorizationGenerator.generateTestS2SToken;

@Component
public class TaskMonitorService {

    private final String serviceAuthorisation;
    private final RestClient restClient;

    private static final String BASE_URL = "http://localhost:8077/monitor/tasks/jobs";

    public TaskMonitorService() {
        this.restClient = RestClient.create(BASE_URL);
        this.serviceAuthorisation = generateTestS2SToken("pcs_api");
    }

    public void triggerInitiationJob() {
        Map<String, Map<String, String>> requestBody = Map.of("job_details", Map.of("name", "INITIATION"));
        initiateJob(requestBody);
    }

    private void initiateJob(Map<String, Map<String, String>> requestBody) {
        restClient
            .post()
            .contentType(APPLICATION_JSON)
            .header("ServiceAuthorization", serviceAuthorisation)
            .body(requestBody)
            .retrieve().toBodilessEntity();
    }
}
