package uk.gov.hmcts.reform.pcs.hearings.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestBody;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.reform.pcs.hearings.model.DeleteHearingRequest;
import uk.gov.hmcts.reform.pcs.hearings.model.GetHearingsResponse;
import uk.gov.hmcts.reform.pcs.hearings.model.HearingRequest;
import uk.gov.hmcts.reform.pcs.hearings.model.HearingResponse;
import uk.gov.hmcts.reform.pcs.hearings.model.UpdateHearingRequest;
import uk.gov.hmcts.reform.pcs.hearings.service.api.HmcHearingApi;
import uk.gov.hmcts.reform.pcs.security.SystemUpdateUser;

@RequiredArgsConstructor
@Service
public class HmcHearingService {

    private final HmcHearingApi hmcHearingApi;
    private final AuthTokenGenerator authTokenGenerator;
    private final SystemUpdateUser systemUpdateUser;
    @Value("${hmc.deployment-id}")
    private String hmctsDeploymentId;

    public HearingResponse createHearing(@RequestBody HearingRequest hearingPayload) {
        return hmcHearingApi.createHearing(systemUpdateUser.getAuthToken(), authTokenGenerator.generate(),
                                           hmctsDeploymentId, hearingPayload);
    }

    public HearingResponse updateHearing(String id, @RequestBody UpdateHearingRequest hearingPayload) {
        return hmcHearingApi.updateHearing(systemUpdateUser.getAuthToken(), authTokenGenerator.generate(),
                                           hmctsDeploymentId, id, hearingPayload);
    }

    public HearingResponse deleteHearing(String id, @RequestBody DeleteHearingRequest hearingDeletePayload) {
        return hmcHearingApi.deleteHearing(systemUpdateUser.getAuthToken(), authTokenGenerator.generate(),
                                           hmctsDeploymentId, id, hearingDeletePayload);
    }

    public GetHearingsResponse getHearing(String id) {
        return hmcHearingApi.getHearing(systemUpdateUser.getAuthToken(), authTokenGenerator.generate(),
                                        hmctsDeploymentId, id, null);
    }
}
