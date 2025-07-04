package uk.gov.hmcts.reform.pcs.ccd.service;

import jakarta.servlet.http.HttpServletRequest;
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
    private final HttpServletRequest request;

    public CoreCaseDataService(
        CoreCaseDataApi coreCaseDataApi, AuthTokenGenerator serviceAuthTokenGenerator,
        IdamService idamService, HttpServletRequest request) {
        this.coreCaseDataApi = coreCaseDataApi;
        this.serviceAuthTokenGenerator = serviceAuthTokenGenerator;
        this.idamService = idamService;
        this.request = request;
    }

    public StartEventResponse startCase(String caseType, String eventId) {
        if (coreCaseDataApi == null) {
            throw new IllegalStateException("CoreCaseDataApi bean not found");
        }

        return  coreCaseDataApi.startCase(
            getUserAuthToken(),
            getServiceToken(),
            caseType,
            eventId
        );
    }

    public CaseDetails submitCaseCreation(String caseType, CaseDataContent caseDataContent) {

        return coreCaseDataApi.submitCaseCreation(
            getUserAuthToken(),
            getServiceToken(),
            caseType,
            caseDataContent
        );

    }

    public StartEventResponse startEvent(String caseId, String eventId) {

        return coreCaseDataApi.startEvent(
            getUserAuthToken(),
            getServiceToken(),
            caseId,
            eventId
        );

    }

    public CaseResource submitEvent(String caseId, CaseDataContent caseDataContent) {
        return coreCaseDataApi.createEvent(
            getUserAuthToken(),
            getServiceToken(),
            caseId,
            caseDataContent
        );

    }

    public CaseDetails getCase(String caseId) {

        return coreCaseDataApi.getCase(
            getUserAuthToken(),
            getServiceToken(),
            caseId
        );
    }

    private String getUserAuthToken() {
        return request.getHeader("Authorization");
    }

    private String getServiceToken() {
        return serviceAuthTokenGenerator.generate();
    }


}
