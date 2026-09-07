package uk.gov.hmcts.reform.pcs.camunda;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(name = "wa-workflow-api", url = "${work-allocation.workflow-api.url}")
public interface WorkAllocationWorkflowApi {
    String SERVICE_AUTHORIZATION = "ServiceAuthorization";

    @PostMapping(
        value = "/workflow/message",
        consumes = MediaType.APPLICATION_JSON_VALUE,
        produces = MediaType.APPLICATION_JSON_VALUE)
    void sendMessage(@RequestHeader(SERVICE_AUTHORIZATION) String serviceAuthorisation,
                     @RequestBody SendMessageRequest sendMessageRequest);

}
