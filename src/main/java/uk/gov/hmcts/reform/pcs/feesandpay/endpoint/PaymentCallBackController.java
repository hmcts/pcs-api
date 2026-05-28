package uk.gov.hmcts.reform.pcs.feesandpay.endpoint;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.reform.pcs.feesandpay.model.PaymentStatusCallback;
import uk.gov.hmcts.reform.pcs.feesandpay.service.PaymentService;

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
    @Operation(
        summary = "This endpoint is invoked by the CCPay service based on the service request callback",
        requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            content = @Content(
                mediaType = APPLICATION_JSON_VALUE,
                examples = @ExampleObject(
                    name = "Paid service request callback",
                    value = """
                    {
                      "service_request_reference": "2023-1692266328473",
                      "ccd_case_number": "1692266325752226",
                      "service_request_amount": 2500,
                      "service_request_status": "Paid",
                      "payment": {
                        "payment_amount": 2500,
                        "payment_reference": "RC-1692-2665-9206-3000",
                        "payment_method": "payment by account",
                        "case_reference": "098DC868",
                        "account_number": "PBA0088311"
                      }
                    }
                    """
                )
            )
        )
    )
    @ApiResponse(
        responseCode = "204",
        description = "Payment callback processed successfully. No response body is returned."
    )
    public ResponseEntity<Void> processPaymentCallback(
        @RequestHeader(value = SERVICE_AUTHORIZATION) String s2sToken,
        @RequestBody String serviceRequestUpdate) throws JsonProcessingException {
        log.info("Payment Callback Received For Case: {}", serviceRequestUpdate);
        processRequestBody(serviceRequestUpdate);
        return ResponseEntity.noContent().build();
    }

    void processRequestBody(String serviceRequestUpdate) throws JsonProcessingException {
        try {
            PaymentStatusCallback update = objectMapper.readValue(serviceRequestUpdate, PaymentStatusCallback.class);
            paymentService.processPaymentResponse(update);
        } catch (JsonProcessingException e) {
            log.error("Failed to process payment update request message", e);
            throw e;
        }
    }

}
