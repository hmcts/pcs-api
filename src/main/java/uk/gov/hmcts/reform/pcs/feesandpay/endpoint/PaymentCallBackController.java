package uk.gov.hmcts.reform.pcs.feesandpay.endpoint;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@RestController
@AllArgsConstructor
@Slf4j
public class PaymentCallBackController {

    public static final String SERVICE_AUTHORIZATION = "ServiceAuthorization";
    public static final String PAYMENT_UPDATE_PATH = "/payment-update";

    private final ObjectMapper objectMapper;
    private final PaymentService paymentService;

    @PutMapping(path = PAYMENT_UPDATE_PATH, consumes = APPLICATION_JSON_VALUE)
    public void processPaymentCallback(
        @RequestHeader(value = SERVICE_AUTHORIZATION) String s2sToken,
        @RequestBody String serviceRequestUpdate) throws JsonProcessingException {
        log.info("Payment Callback Received For Case: {}", serviceRequestUpdate);
        processRequestBody(serviceRequestUpdate);
    }

    void processRequestBody(String serviceRequestUpdate) throws JsonProcessingException {
        try {
            ServiceRequestUpdate update = objectMapper.readValue(serviceRequestUpdate, ServiceRequestUpdate.class);
            paymentService.processPaymentResponse(update);
        } catch (JsonProcessingException e) {
            log.error("Failed to process payment update request message", e);
            throw e;
        }
    }

}
