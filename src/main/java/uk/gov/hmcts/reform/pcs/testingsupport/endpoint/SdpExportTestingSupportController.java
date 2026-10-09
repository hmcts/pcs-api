package uk.gov.hmcts.reform.pcs.testingsupport.endpoint;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.reform.pcs.testingsupport.model.SdpExportRoundTripResult;
import uk.gov.hmcts.reform.pcs.testingsupport.service.SdpExportRoundTripService;

/**
 * Test-support only: lets the functional tests prove the deployed pod can write to and read from the
 * SDP export container with its workload identity. The test blob is left in the container.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/testing-support")
@ConditionalOnProperty(name = {"testing-support.enabled", "sdp-export.enabled"}, havingValue = "true")
@Tag(name = "Testing Support")
public class SdpExportTestingSupportController {

    private final SdpExportRoundTripService sdpExportRoundTripService;

    @Operation(
        summary = "Round-trip a test blob through the SDP export container",
        description = "Writes a JSON blob named after the current UTC time under functional-test/, reads it "
            + "back and checks the content. The blob is left in the container. Failures return 500 with the "
            + "Azure error message."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Blob written and read back"),
        @ApiResponse(responseCode = "401", description = "Unauthorized - Invalid or missing authorization token"),
        @ApiResponse(responseCode = "403", description = "Forbidden - Invalid or missing service authorization token"),
        @ApiResponse(responseCode = "500", description = "Round trip failed; body carries the error")
    })
    @PostMapping("/sdp-export/round-trip")
    public ResponseEntity<SdpExportRoundTripResult> roundTrip(
        @RequestHeader(value = "ServiceAuthorization") String serviceAuthorization) {

        SdpExportRoundTripResult result = sdpExportRoundTripService.roundTrip();
        HttpStatus status = result.roundTrip() ? HttpStatus.OK : HttpStatus.INTERNAL_SERVER_ERROR;
        return ResponseEntity.status(status).body(result);
    }
}
