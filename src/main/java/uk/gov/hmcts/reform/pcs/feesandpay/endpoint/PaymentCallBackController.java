package uk.gov.hmcts.reform.pcs.feesandpay.endpoint;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.reform.pcs.feesandpay.model.ServiceRequestUpdate;
import uk.gov.hmcts.reform.pcs.feesandpay.service.PaymentService;

import static com.azure.core.http.ContentType.APPLICATION_JSON;

@RestController
@AllArgsConstructor
public class PaymentCallBackController {

    private final PaymentService paymentService;

    @PostMapping(path = "/service-request-update", consumes = APPLICATION_JSON)
    @Operation(description = "Callback to create Fee and Pay service request")
    public void ccdSubmitted(
        @RequestHeader(HttpHeaders.AUTHORIZATION) @Parameter(hidden = true) String authorisation,
        @RequestHeader("ServiceAuthorization") String s2sToken,
        @RequestBody ServiceRequestUpdate serviceRequestUpdate) {

        paymentService.processPaymentResponse(serviceRequestUpdate);

    }

}
