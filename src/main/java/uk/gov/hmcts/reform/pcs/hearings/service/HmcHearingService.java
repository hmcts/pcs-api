package uk.gov.hmcts.reform.pcs.hearings.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
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
    @Value("${core_case_data.api.external-url}")
    private String dataStoreUrl;
    @Value("${role-assignment.api.url}")
    private String roleAssignmentUrl;
    // TODO: remove once pcs-api system user has hearing-manager role assigned in RAS
    @Value("${hmc.temp-user-token:}")
    private String tempUserToken;
    @Value("${hmc.temp-s2s-token:}")
    private String tempS2sToken;

    public HearingResponse createHearing(@RequestBody HearingRequest hearingPayload) {
        return hmcHearingApi.createHearing(getAuthorisation(), authTokenGenerator.generate(),
                                           hmctsDeploymentId, dataStoreUrl, roleAssignmentUrl, hearingPayload);
    }

    public HearingResponse updateHearing(String id, @RequestBody UpdateHearingRequest hearingPayload) {
        return hmcHearingApi.updateHearing(getAuthorisation(), authTokenGenerator.generate(),
                                           hmctsDeploymentId, dataStoreUrl, roleAssignmentUrl, id, hearingPayload);
    }

    public HearingResponse deleteHearing(String id, @RequestBody DeleteHearingRequest hearingDeletePayload) {
        return hmcHearingApi.deleteHearing(getAuthorisation(), authTokenGenerator.generate(), hmctsDeploymentId,
            dataStoreUrl, roleAssignmentUrl, id, hearingDeletePayload);
    }

    public GetHearingsResponse getHearing(String id) {
        return hmcHearingApi.getHearing(getAuthorisation(), authTokenGenerator.generate(),
                                        hmctsDeploymentId, dataStoreUrl, roleAssignmentUrl, id, null);
    }

    private String getAuthorisation() {
        return StringUtils.hasText(tempUserToken) ? tempUserToken : idamService.getSystemUserAuthorisation();
    }

    private String getServiceAuthorisation() {
        return StringUtils.hasText(tempS2sToken) ? tempS2sToken : getServiceAuthorisation();
    }
}
