package uk.gov.hmcts.reform.pcs.hearings.endpoint;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.reform.pcs.hearings.model.HearingValuesRequest;
import uk.gov.hmcts.reform.pcs.hearings.model.ServiceHearingRequest;
import uk.gov.hmcts.reform.pcs.hearings.model.ServiceHearingValues;
//import uk.gov.hmcts.reform.civil.service.hearings.HearingValuesService;
import uk.gov.hmcts.reform.pcs.hearings.model.ServiceLinkedCases;

@Tag(name = "Hearing Values Controller")
@Slf4j
@RestController
@AllArgsConstructor
public class ServiceHearingController {

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
    public ResponseEntity<ServiceHearingValues> getHearingValues(
        @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization,
        @NonNull @RequestBody HearingValuesRequest requestDetails) throws Exception {

        //TODO: populate the data
        ServiceHearingValues hearingValues = new ServiceHearingValues();

        return new ResponseEntity<>(hearingValues, HttpStatus.OK);
    }


    @PostMapping(path = {
        "/serviceLinkedCases"
    })
    @Operation(summary = "Get linked cases for a Case and it's Hearing")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "OK"),
        @ApiResponse(responseCode = "401", description = "Incorrect authorisation"),
        @ApiResponse(responseCode = "400", description = "Bad Request")
    })
    public ResponseEntity<List<ServiceLinkedCases>> getLinkedCases(
        @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization,
        @NonNull @RequestBody ServiceHearingRequest requestDetails) throws Exception {

        //TODO: populate the data
        List<ServiceLinkedCases> hearingValues = new ArrayList<>();

        return new ResponseEntity<>(hearingValues, HttpStatus.OK);
    }
}