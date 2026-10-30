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

import uk.gov.hmcts.reform.pcs.hearings.model.HearingLocation;
import uk.gov.hmcts.reform.pcs.hearings.model.HearingValuesRequest;
import uk.gov.hmcts.reform.pcs.hearings.model.IndividualDetails;
import uk.gov.hmcts.reform.pcs.hearings.model.PanelRequirements;
import uk.gov.hmcts.reform.pcs.hearings.model.PartyDetails;
import uk.gov.hmcts.reform.pcs.hearings.model.ServiceHearingRequest;
import uk.gov.hmcts.reform.pcs.hearings.model.ServiceHearingValues;
import uk.gov.hmcts.reform.pcs.hearings.model.ServiceLinkedCases;

@Tag(name = "Hearing Values Controller")
@Slf4j
@RestController
@AllArgsConstructor
public class ServiceHearingController {

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

        // set defaults
        List<PartyDetails> parties = List.of(
            PartyDetails.builder()
                .individualDetails(IndividualDetails.builder()
                    .preferredHearingChannel("INTER")
                    .build()
            ).build()
        );

        ServiceHearingValues hearingValues = ServiceHearingValues.builder()
            .caseId(requestDetails.getCaseReference() + "")
            .hmctsServiceID("AAA3")
            .caseAdditionalSecurityFlag(false)
            .caseRestrictedFlag(false)
            .autoListFlag(false)
            .hearingInWelshFlag(false)
            .privateHearingRequiredFlag(false)
            .hearingIsLinkedFlag(false)
            .hearingLocations(List.of(
                HearingLocation.builder()
                    .locationType("Court")
                    .build()
            ))
            .panelRequirements(PanelRequirements.builder()
                .roleType(List.of("24", "45")) // 24 = District Judge, 45 = Deputy District Judge- Fee-Paid
                .build() 
            )
            .parties(parties)
            .build();

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

        // Need to load linked cases from PCS

        List<ServiceLinkedCases> linkedCases = new ArrayList<>();

        return new ResponseEntity<>(linkedCases, HttpStatus.OK);
    }
}
