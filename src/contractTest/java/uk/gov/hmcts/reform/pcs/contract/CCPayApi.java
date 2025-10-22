package uk.gov.hmcts.reform.pcs.contract;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

import au.com.dius.pact.consumer.dsl.DslPart;

public interface CCPayApi {

    @PostMapping("/service-request")
    String postServiceRequest(
        @RequestHeader("Bearer Authorization") String authorisation,
        @RequestHeader("ServiceAuthorization") String serviceAuthorization,
        @RequestBody DslPart requestBody);
}
