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

        //TODO: populate the data

        //ServiceLinkedCases linkedCase = new ServiceLinkedCases();
        //linkedCase.setCaseName("A vs B");
        //linkedCase.setCaseReference("1793287276819762");
        //linkedCase.setReasonsForLink(List.of("2nd hearing"));

        List<ServiceLinkedCases> linkedCases = new ArrayList<>();

        //linkedCases.add(linkedCase);

        return new ResponseEntity<>(linkedCases, HttpStatus.OK);
    }
}
