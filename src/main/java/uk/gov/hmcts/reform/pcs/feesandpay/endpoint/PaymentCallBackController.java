package uk.gov.hmcts.reform.pcs.feesandpay.endpoint;

import lombok.AllArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.reform.pcs.feesandpay.model.ServiceRequestUpdate;
import uk.gov.hmcts.reform.pcs.feesandpay.service.PaymentService;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@RestController
@AllArgsConstructor
public class PaymentCallBackController {

    public static final String SERVICE_AUTHORIZATION = "ServiceAuthorization";

    private final PaymentService paymentService;

    @PostMapping(path = "/service-request-update", consumes = APPLICATION_JSON_VALUE)
    public void processPaymentCallback(
        @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorisation,
        @RequestHeader(value = SERVICE_AUTHORIZATION, required = false) String s2sToken,
        @RequestBody ServiceRequestUpdate serviceRequestUpdate) {
        paymentService.processPaymentResponse(serviceRequestUpdate);
    }

}
