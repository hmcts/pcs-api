package uk.gov.hmcts.reform.pcs.ccd.service;

import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.reform.ccd.client.CoreCaseDataApi;
import uk.gov.hmcts.reform.ccd.client.model.CaseDataContent;
import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;
import uk.gov.hmcts.reform.ccd.client.model.StartEventResponse;
import uk.gov.hmcts.reform.idam.client.IdamClient;
import uk.gov.hmcts.reform.pcs.ccd.repository.GeneralApplicationRepository;
import uk.gov.hmcts.reform.pcs.idam.IdamService;

@Service
public class CoreCaseDataService {

    @Autowired
    private IdamClient idamClient;

    private final CoreCaseDataApi coreCaseDataApi;
    private final GeneralApplicationRepository generalApplicationRepository;
    private final ModelMapper modelMapper;
    private final AuthTokenGenerator serviceAuthTokenGenerator;
    private final IdamService idamService;
// private final AuthTokenGenerator serviceAuthTokenGenerator;

    public CoreCaseDataService(CoreCaseDataApi coreCaseDataApi,
                               GeneralApplicationRepository generalApplicationRepository,
                               ModelMapper modelMapper, AuthTokenGenerator serviceAuthTokenGenerator,
                               IdamService idamService) {
        this.coreCaseDataApi = coreCaseDataApi;
        this.generalApplicationRepository = generalApplicationRepository;
        this.modelMapper = modelMapper;
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

    public CaseDetails submitCaseUpdate(String caseId, CaseDataContent caseDataContent) {

//        return coreCaseDataApi.startEvent(
//            getServiceToken(),
//            getServiceToken(),
//            caseType,
//            caseDataContent
//
//        )
        return null;
    }
    private String getAuthToken(){
       return idamClient.getAccessToken("caseworker@pcs.com", "password");
    }

    //String userAuthToken = idamService.getSystemUserAuthorisation();
    //String serviceToken = serviceAuthTokenGenerator.generate();
    //String userId = idamService.validateAuthToken(userAuthToken).getUserDetails().getUid();

    private String getServiceToken(){
        return "Bearer eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJjY2RfZ3ciLCJpYXQiOjE2" +
            "ODAwMDAwMDB9.4QdWwz5ZQ5p5v3cJXkQ4lQwQkR2o9Yw5Qw8yQw8yQw8";
    }

}
