package uk.gov.hmcts.reform.pcs.hearings.service.api;

import static org.springframework.http.HttpHeaders.AUTHORIZATION;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import uk.gov.hmcts.reform.pcs.hearings.model.DeleteHearingRequest;
import uk.gov.hmcts.reform.pcs.hearings.model.HearingRequest;
import uk.gov.hmcts.reform.pcs.hearings.model.HearingResponse;
import uk.gov.hmcts.reform.pcs.hearings.model.UpdateHearingRequest;
import uk.gov.hmcts.reform.pcs.hearings.model.GetHearingsResponse;


@SuppressWarnings({"PMD.UseObjectForClearerAPI"})
@FeignClient(name = "hmc-hearing", url = "${hmc.api-url}")
public interface HmcHearingApi {

    String SERVICE_AUTHORIZATION = "ServiceAuthorization";
    String HEARING_ENDPOINT = "/hearing";
    String ID = "id";
    String HMCTS_DEPLOYMENT_ID = "hmctsDeploymentId";

    @PostMapping(value = HEARING_ENDPOINT, consumes = MediaType.APPLICATION_JSON_VALUE)
    HearingResponse createHearing(
        @RequestHeader(AUTHORIZATION) String authorisation,
        @RequestHeader(SERVICE_AUTHORIZATION) String serviceAuthorization,
        @RequestHeader(value = HMCTS_DEPLOYMENT_ID, required = false) String hmctsDeploymentId,
        @RequestBody HearingRequest hearingPayload
    );

    @PutMapping(value = HEARING_ENDPOINT + "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    HearingResponse updateHearing(
        @RequestHeader(AUTHORIZATION) String authorisation,
        @RequestHeader(SERVICE_AUTHORIZATION) String serviceAuthorization,
        @RequestHeader(value = HMCTS_DEPLOYMENT_ID, required = false) String hmctsDeploymentId,
        @PathVariable String id,
        @RequestBody UpdateHearingRequest hearingPayload
    );

    @DeleteMapping(value = HEARING_ENDPOINT + "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    HearingResponse deleteHearing(
        @RequestHeader(AUTHORIZATION) String authorisation,
        @RequestHeader(SERVICE_AUTHORIZATION) String serviceAuthorization,
        @RequestHeader(value = HMCTS_DEPLOYMENT_ID, required = false) String hmctsDeploymentId,
        @PathVariable String id,
        @RequestBody DeleteHearingRequest hearingDeletePayload
    );

    @GetMapping(HEARING_ENDPOINT + "/{id}")
    GetHearingsResponse getHearing(
        @RequestHeader(AUTHORIZATION) String authorisation,
        @RequestHeader(SERVICE_AUTHORIZATION) String serviceAuthorization,
        @RequestHeader(value = HMCTS_DEPLOYMENT_ID, required = false) String hmctsDeploymentId,
        @PathVariable String id,
        @RequestParam(name = "isValid", required = false) Boolean isValid
    );
}
