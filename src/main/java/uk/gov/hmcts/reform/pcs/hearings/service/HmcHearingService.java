package uk.gov.hmcts.reform.pcs.hearings.service;

import static org.springframework.http.HttpHeaders.AUTHORIZATION;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.reform.pcs.hearings.model.DeleteHearingRequest;
import uk.gov.hmcts.reform.pcs.hearings.model.GetHearingsResponse;
import uk.gov.hmcts.reform.pcs.hearings.model.HearingRequest;
import uk.gov.hmcts.reform.pcs.hearings.model.HearingResponse;
import uk.gov.hmcts.reform.pcs.hearings.model.UpdateHearingRequest;
import uk.gov.hmcts.reform.pcs.hearings.service.api.HmcHearingApi;

@RequiredArgsConstructor
@Service
public class HmcHearingService {

    private final HmcHearingApi hmcHearingApi;
    private final AuthTokenGenerator authTokenGenerator;
    @Value("${hmc.deployment-id}")
    private String hmctsDeploymentId;

    public HearingResponse createHearing(
        @RequestHeader(AUTHORIZATION) String authorisation,
        @RequestBody HearingRequest hearingPayload) {
        return hmcHearingApi.createHearing(authorisation, authTokenGenerator.generate(),
                                          hmctsDeploymentId, hearingPayload);
    }

    public HearingResponse updateHearing(
        @RequestHeader(AUTHORIZATION) String authorisation,
        @PathVariable("id") String id,
        @RequestBody UpdateHearingRequest hearingPayload) {
        return hmcHearingApi.updateHearing(authorisation, authTokenGenerator.generate(),
                                           hmctsDeploymentId, id, hearingPayload);
    }

    public HearingResponse deleteHearing(
        @RequestHeader(AUTHORIZATION) String authorisation,
        @PathVariable("id") String id,
        @RequestBody DeleteHearingRequest hearingDeletePayload) {
        return hmcHearingApi.deleteHearing(authorisation, authTokenGenerator.generate(),
                                           hmctsDeploymentId, id, hearingDeletePayload);
    }

    public GetHearingsResponse getHearing(
        @RequestHeader(AUTHORIZATION) String authorisation,
        @PathVariable("id") String id) {
        return hmcHearingApi.getHearing(authorisation, authTokenGenerator.generate(),
                                        hmctsDeploymentId,id, null);
    }
}
