package uk.gov.hmcts.reform.pcs.feesandpay.api;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import uk.gov.hmcts.reform.pcs.feesandpay.model.FeeResponse;

@FeignClient(name = "fees-register-api", url = "${fees-register.api.url}")
public interface FeesRegisterApi {

    String SERVICE_AUTHORIZATION_HEADER = "ServiceAuthorization";

    /**
     * Looks up a fee based on the provided criteria.
     *
     * @param serviceAuthorization Service-to-service authorization token
     * @param channel The channel (e.g., "online", "default")
     * @param event The event type
     * @param jurisdiction1 Primary jurisdiction
     * @param jurisdiction2 Secondary jurisdiction
     * @param keyword Optional keyword for fee lookup
     * @param service The service name
     * @return Fee response containing code, description, version, and amount
     */
    @GetMapping("/fees-register/fees/lookup")
    FeeResponse lookupFee(
        @RequestHeader(SERVICE_AUTHORIZATION_HEADER) String serviceAuthorization,
        @RequestParam(name = "service") String service,
        @RequestParam(name = "jurisdiction1") String jurisdiction1,
        @RequestParam(name = "jurisdiction2") String jurisdiction2,
        @RequestParam(name = "channel") String channel,
        @RequestParam(name = "event") String event,
        @RequestParam(name = "applicantType") String applicantType,
        @RequestParam(name = "amountOrVolume") String amountOrVolume,
        @RequestParam(name = "keyword", required = false) String keyword
    );
}
