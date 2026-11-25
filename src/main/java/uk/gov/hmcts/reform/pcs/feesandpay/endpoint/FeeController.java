package uk.gov.hmcts.reform.pcs.feesandpay.endpoint;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.reform.pcs.controllers.RestExceptionHandler;
import uk.gov.hmcts.reform.pcs.feesandpay.exception.FeeNotFoundException;
import uk.gov.hmcts.reform.pcs.feesandpay.model.ClaimIssueFeeResponse;
import uk.gov.hmcts.reform.pcs.feesandpay.model.FeeDetails;
import uk.gov.hmcts.reform.pcs.feesandpay.model.FeeType;
import uk.gov.hmcts.reform.pcs.feesandpay.service.FeeService;

import java.math.RoundingMode;

@Slf4j
@RestController
@RequestMapping("/fees")
@RequiredArgsConstructor
@Tag(name = "Fees", description = "Fees from the fees register that do not belong to a case")
public class FeeController {

    private static final int POUNDS_AND_PENCE = 2;

    private final FeeService feeService;

    @GetMapping(value = "/claim-issue", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
        summary = "Get the fee to issue a possession claim",
        description = "Looks the fee up in the fees register. Needs only a service token, so it can be read "
            + "before a citizen signs in",
        security = @SecurityRequirement(name = "ServiceAuthorization")
    )
    @ApiResponse(responseCode = "200", description = "The claim issue fee")
    @ApiResponse(responseCode = "403", description = "Invalid Service Authorization")
    @ApiResponse(responseCode = "503", description = "The fees register could not give the fee")
    public ResponseEntity<?> getClaimIssueFee(
        @Parameter(description = "Service-to-Service (S2S) authorization token", required = true)
        @RequestHeader("ServiceAuthorization") String s2sToken
    ) {
        try {
            FeeDetails fee = feeService.getFee(FeeType.CASE_ISSUE_FEE);
            return ResponseEntity.ok(new ClaimIssueFeeResponse(
                fee.getFeeAmount().setScale(POUNDS_AND_PENCE, RoundingMode.HALF_UP).toPlainString(),
                fee.getCode(),
                fee.getVersion()
            ));
        } catch (FeeNotFoundException e) {
            log.error("Claim issue fee unavailable", e);
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(new RestExceptionHandler.Error("The claim fee is unavailable"));
        }
    }
}
