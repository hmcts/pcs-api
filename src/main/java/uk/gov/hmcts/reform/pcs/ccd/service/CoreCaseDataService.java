package uk.gov.hmcts.reform.pcs.ccd.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.reform.ccd.client.CoreCaseDataApi;
import uk.gov.hmcts.reform.ccd.client.model.CaseDataContent;
import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;
import uk.gov.hmcts.reform.ccd.client.model.CaseResource;
import uk.gov.hmcts.reform.ccd.client.model.StartEventResponse;
import uk.gov.hmcts.reform.idam.client.IdamClient;
import uk.gov.hmcts.reform.pcs.idam.IdamService;

@Service
public class CoreCaseDataService {

    @Autowired
    private IdamClient idamClient;

    private final CoreCaseDataApi coreCaseDataApi;
    private final AuthTokenGenerator serviceAuthTokenGenerator;
    private final IdamService idamService;


    public CoreCaseDataService(CoreCaseDataApi coreCaseDataApi,
                               AuthTokenGenerator serviceAuthTokenGenerator,
                               IdamService idamService) {
        this.coreCaseDataApi = coreCaseDataApi;
        this.serviceAuthTokenGenerator = serviceAuthTokenGenerator;
        this.idamService = idamService;
    }

    public StartEventResponse startCase(String caseType, String eventId) {

        return  coreCaseDataApi.startCase(
            getAuthToken(),
            getServiceToken(),
            caseType,
            eventId
        );
    }

    public CaseDetails submitCaseCreation(String caseType, CaseDataContent caseDataContent) {

        return coreCaseDataApi.submitCaseCreation(
            getAuthToken(),
            getServiceToken(),
            caseType,
            caseDataContent
        );

    }

    public StartEventResponse startEvent(String caseId, String eventId) {

        return coreCaseDataApi.startEvent(
            getAuthToken(),
            getServiceToken(),
            caseId,
            eventId
        );

    }

    public CaseResource submitEvent(String caseId, CaseDataContent caseDataContent) {
        return coreCaseDataApi.createEvent(
            getAuthToken(),
            getServiceToken(),
            caseId,
            caseDataContent
        );

    }

    public CaseDetails getCase(String caseId) {

        return coreCaseDataApi.getCase(
            getAuthToken(),
            getServiceToken(),
            caseId
        );
    }

    private String getAuthToken() {
        return idamClient.getAccessToken("caseworker@pcs.com", "password");
    }

    private String getServiceToken() {
        return serviceAuthTokenGenerator.generate();
    }


}
