package uk.gov.hmcts.reform.pcs.feesandpay.api;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.cloud.openfeign.FeignClientProperties.FeignClientConfiguration;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import uk.gov.hmcts.reform.pcs.feesandpay.model.ServiceRequestBody;
import uk.gov.hmcts.reform.pcs.feesandpay.model.ServiceRequestResponse;

@FeignClient(
    name = "service-request-api",
    url = "${payment.url}",
    configuration = FeignClientConfiguration.class
)
public interface PaymentApi {

    @PostMapping(value = "/service-request", consumes = "application/json")
    ServiceRequestResponse createServiceRequest(
        @RequestHeader(value = "Authorization") String authorization,
        @RequestHeader(value = "serviceAuthorization") String serviceAuthorization,
        @RequestBody ServiceRequestBody requestBody
        );
}
