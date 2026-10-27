package uk.gov.hmcts.reform.pcs.hearings.endpoint;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.reform.pcs.hearings.model.HearingValuesRequest;
import uk.gov.hmcts.reform.pcs.hearings.model.CaseDetails;
//import uk.gov.hmcts.reform.civil.service.hearings.HearingValuesService;

@Tag(name = "Hearing Values Controller")
@Slf4j
@RestController
@AllArgsConstructor
public class HearingValuesController {

    //private final HearingValuesService hearingValuesService;

    @PostMapping(path = {
        "/serviceHearingValues"
    })
    @Operation(summary = "Builds and returns the hearing values for exui")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "OK"),
        @ApiResponse(responseCode = "401", description = "Incorrect authorisation"),
        @ApiResponse(responseCode = "400", description = "Bad Request")
    })
    public ResponseEntity<CaseDetails> getHearingValues(
        @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization,
        @NonNull @RequestBody HearingValuesRequest requestDetails) throws Exception {

        log.info(
            "\n\n\n\n\n\nReceived serviceHearingValues request from XUI: caseReference={}, hearingId={}\n\n\n\n\n\n",
            requestDetails.getCaseReference(),
            requestDetails.getHearingId()
        );

        System.out.print(
            "\n\n\n\n\n\nReceived serviceHearingValues request from XUI: caseReference=" 
            + requestDetails.getCaseReference() + ", hearingId=" + requestDetails.getHearingId() + "\n\n\n\n\n\n"
        );

        CaseDetails hearingValues = new CaseDetails();

        log.info(
            "\n\n\n\n\n\nReturning serviceHearingValues response: caseReference={}\n\n\n\n\n\n",
            requestDetails.getCaseReference()
        );
        System.out.println(
            "\n\n\n\n\n\nReturning serviceHearingValues response: caseReference=" 
            + requestDetails.getCaseReference() + "\n\n\n\n\n\n"
        );
        return new ResponseEntity<>(hearingValues, HttpStatus.OK);
    }
}