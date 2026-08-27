package uk.gov.hmcts.reform.pcs.feesandpay.endpoint;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.reform.pcs.feesandpay.model.CardPaymentStatusResponse;
import uk.gov.hmcts.reform.pcs.feesandpay.model.CreateCardPaymentRequest;
import uk.gov.hmcts.reform.pcs.feesandpay.model.CreateCardPaymentResponse;
import uk.gov.hmcts.reform.pcs.feesandpay.model.PbaAccountsResponse;
import uk.gov.hmcts.reform.pcs.feesandpay.model.PbaPaymentRequest;
import uk.gov.hmcts.reform.pcs.feesandpay.model.PbaPaymentResponse;
import uk.gov.hmcts.reform.pcs.feesandpay.service.PaymentService;

import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@RestController
@RequestMapping("/payment")
@Tag(name = "Payment")
@AllArgsConstructor
@Slf4j
public class PaymentController {

    private static final String SERVICE_AUTHORIZATION = "ServiceAuthorization";

    private final PaymentService paymentService;

    @PostMapping(path = "service-request/{serviceRequestReference}/card-payment", consumes = APPLICATION_JSON_VALUE)
    @Operation(
        summary = "Create a payment request",
        requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            content = @Content(
                mediaType = APPLICATION_JSON_VALUE,
                examples = @ExampleObject(
                    name = "Create payment request",
                    value = """
                    {
                        "amount": 300.99,
                        "language": "English",
                        "returnUrl": "https://some-frontend/payment-return-url"
                    }
                    """
                )
            )
        )
    )
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Payment request created successfully"),
        @ApiResponse(responseCode = "403", description = "Forbidden - Invalid or missing service authorization token"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<CreateCardPaymentResponse> createPaymentRequest(
        @RequestHeader(value = SERVICE_AUTHORIZATION) String s2sToken,
        @PathVariable("serviceRequestReference") String serviceRequestReference,
        @RequestBody @Valid CreateCardPaymentRequest cardPaymentRequest) {

        CreateCardPaymentResponse createPaymentResponse = paymentService.createPaymentRequest(serviceRequestReference,
            cardPaymentRequest);

        return ResponseEntity.status(HttpStatus.CREATED).body(createPaymentResponse);
    }

    @GetMapping(path = "card-payment/{internalReference}/status", produces = APPLICATION_JSON_VALUE)
    @Operation(summary = "Get status of a card payment")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Payment status found"),
        @ApiResponse(responseCode = "403", description = "Forbidden - Invalid or missing service authorization token"),
        @ApiResponse(responseCode = "404", description = "Payment status not found"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<CardPaymentStatusResponse> getCardPaymentStatus(
        @RequestHeader(value = SERVICE_AUTHORIZATION) String s2sToken,
        @PathVariable("internalReference") String internalReference) {

        CardPaymentStatusResponse statusResponse = paymentService.getPaymentStatus(internalReference);

        return ResponseEntity.ok(statusResponse);
    }

    @GetMapping(path = "pba-accounts")
    @Operation(
        summary = "Get list of PBA account details"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "PBA accounts returned successfully"),
        @ApiResponse(responseCode = "403", description = "Forbidden - Invalid or missing service authorization token"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<PbaAccountsResponse> getPbaAccounts(
        @RequestHeader(value = AUTHORIZATION) String authorization,
        @RequestHeader(value = SERVICE_AUTHORIZATION) String s2sToken) {

        PbaAccountsResponse pbaAccountsResponse = paymentService.getPbaAccounts(authorization);

        return ResponseEntity.status(HttpStatus.OK).body(pbaAccountsResponse);
    }

    @PostMapping(path = "service-request/{serviceRequestReference}/pba", consumes = APPLICATION_JSON_VALUE)
    @Operation(
        summary = "Create a PBA payment request",
        requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            content = @Content(
                mediaType = APPLICATION_JSON_VALUE,
                examples = @ExampleObject(
                    name = "Create PBA payment request",
                    value = """
                    {
                        "amount": 300.99,
                        "pbaAccount": "PBA123",
                        "customerReference": "abc"
                    }
                    """
                )
            )
        )
    )
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Payment request created successfully"),
        @ApiResponse(responseCode = "403", description = "Forbidden - Invalid or missing service authorization token"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<PbaPaymentResponse> createPbaPaymentRequest(
        @RequestHeader(value = SERVICE_AUTHORIZATION) String s2sToken,
        @RequestHeader(value = AUTHORIZATION) String authorization,
        @PathVariable("serviceRequestReference") String serviceRequestReference,
        @RequestBody @Valid PbaPaymentRequest pbaPaymentRequest) {

        PbaPaymentResponse pbaPaymentResponse = paymentService.createPbaPaymentRequest(
            authorization,
            serviceRequestReference,
            pbaPaymentRequest
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(pbaPaymentResponse);
    }

}
