package uk.gov.hmcts.reform.pcs.camunda;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

import java.util.Map;

/** Work Allocation's task management, called as the user, as XUI calls it. */
@FeignClient(name = "wa-task-management-api", url = "${work-allocation.task-management.url}")
public interface TaskManagementApi {

    @PostMapping(value = "/task/{taskId}/complete", consumes = MediaType.APPLICATION_JSON_VALUE)
    void completeTask(@RequestHeader("Authorization") String authorisation,
                      @RequestHeader("ServiceAuthorization") String serviceAuthorisation,
                      @PathVariable("taskId") String taskId,
                      @RequestBody Map<String, Object> request);
}
