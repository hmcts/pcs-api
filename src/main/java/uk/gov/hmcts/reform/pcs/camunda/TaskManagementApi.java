package uk.gov.hmcts.reform.pcs.camunda;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.Map;

/**
 * Work Allocation's task management, called as the user, as XUI calls it: searching the tasks they
 * can see, and completing one.
 */
@FeignClient(name = "wa-task-management-api", url = "${work-allocation.task-management.url}")
public interface TaskManagementApi {
    String SERVICE_AUTHORIZATION = "ServiceAuthorization";
    String AUTHORIZATION = "Authorization";

    @PostMapping(
        value = "/task",
        consumes = MediaType.APPLICATION_JSON_VALUE,
        produces = MediaType.APPLICATION_JSON_VALUE)
    TaskSearchResponse searchTasks(@RequestHeader(AUTHORIZATION) String authorisation,
                                   @RequestHeader(SERVICE_AUTHORIZATION) String serviceAuthorisation,
                                   @RequestParam("first_result") int firstResult,
                                   @RequestParam("max_results") int maxResults,
                                   @RequestBody TaskSearchRequest request);

    @PostMapping(value = "/task/{taskId}/complete", consumes = MediaType.APPLICATION_JSON_VALUE)
    void completeTask(@RequestHeader(AUTHORIZATION) String authorisation,
                      @RequestHeader(SERVICE_AUTHORIZATION) String serviceAuthorisation,
                      @PathVariable("taskId") String taskId,
                      @RequestBody Map<String, Object> request);

    record TaskSearchRequest(@JsonProperty("search_parameters") List<SearchParameter> searchParameters) {
    }

    record SearchParameter(String key, String operator, List<String> values) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record TaskSearchResponse(List<Task> tasks) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Task(String id,
                String type,
                @JsonProperty("additional_properties") Map<String, String> additionalProperties) {
    }
}
