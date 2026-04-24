package uk.gov.hmcts.reform.pcs.feesandpay.endpoint;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.reform.pcs.feesandpay.service.PaymentService;

import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@RestController
@AllArgsConstructor
@Slf4j
public class PaymentCallBackController {

    public static final String SERVICE_AUTHORIZATION = "ServiceAuthorization";
    public static final String PAYMENT_UPDATE_PATH = "/payment-update";

    private final PaymentService paymentService;

    @PutMapping(path = PAYMENT_UPDATE_PATH, consumes = APPLICATION_JSON_VALUE)
    public void processPaymentCallback(
        @RequestHeader(value = AUTHORIZATION) String authorisation,
        @RequestHeader(value = SERVICE_AUTHORIZATION) String s2sToken,
        @RequestBody String serviceRequestUpdate) {
        log.info("Payment Callback Received For Case: {}", serviceRequestUpdate);

    }

}
