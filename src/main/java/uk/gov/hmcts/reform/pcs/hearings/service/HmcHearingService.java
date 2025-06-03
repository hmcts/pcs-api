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
import uk.gov.hmcts.reform.pcs.idam.IdamService;

@RequiredArgsConstructor
@Service
public class HmcHearingService {

    private final HmcHearingApi hmcHearingApi;
    private final AuthTokenGenerator authTokenGenerator;
    private final IdamService idamService;
    @Value("${hmc.deployment-id}")
    private String hmctsDeploymentId;

    public HearingResponse createHearing(@RequestBody HearingRequest hearingPayload) {
        return hmcHearingApi.createHearing(idamService.getSystemUserAuthorisation(), authTokenGenerator.generate(),
                                           hmctsDeploymentId, hearingPayload);
    }

    public HearingResponse updateHearing(String id, @RequestBody UpdateHearingRequest hearingPayload) {
        return hmcHearingApi.updateHearing(idamService.getSystemUserAuthorisation(), authTokenGenerator.generate(),
                                           hmctsDeploymentId, id, hearingPayload);
    }

    public HearingResponse deleteHearing(String id, @RequestBody DeleteHearingRequest hearingDeletePayload) {
        return hmcHearingApi.deleteHearing(idamService.getSystemUserAuthorisation(), authTokenGenerator.generate(),
                                           hmctsDeploymentId, id, hearingDeletePayload);
    }

    public GetHearingsResponse getHearing(String id) {
        return hmcHearingApi.getHearing(idamService.getSystemUserAuthorisation(), authTokenGenerator.generate(),
                                        hmctsDeploymentId, id, null);
    }
}
