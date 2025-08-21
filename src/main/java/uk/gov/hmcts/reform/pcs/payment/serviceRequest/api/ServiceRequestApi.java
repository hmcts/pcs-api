package uk.gov.hmcts.reform.pcs.payment.serviceRequest.api;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import uk.gov.hmcts.reform.pcs.payment.serviceRequest.model.ServiceRequestRequest;
import uk.gov.hmcts.reform.pcs.payment.serviceRequest.model.ServiceRequestResponse;

import static org.springframework.http.HttpHeaders.AUTHORIZATION;

@FeignClient(name = "service-request-api", url = "${payment.api.url}")
public interface ServiceRequestApi {

    @PostMapping(value = "/service-request", consumes = "application/json")
    ServiceRequestResponse createServiceRequest(
        @RequestHeader(value = AUTHORIZATION) String authorisation,
        @RequestHeader(value = "ServiceAuthorization") String serviceAuthorization,
        @RequestBody ServiceRequestRequest serviceRequestRequest
    );
}
