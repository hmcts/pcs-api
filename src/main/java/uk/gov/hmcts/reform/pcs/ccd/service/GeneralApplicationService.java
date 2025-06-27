package uk.gov.hmcts.reform.pcs.ccd.service;

import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.reform.ccd.client.CoreCaseDataApi;
import uk.gov.hmcts.reform.ccd.client.model.CaseDataContent;
import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;
import uk.gov.hmcts.reform.ccd.client.model.Event;
import uk.gov.hmcts.reform.ccd.client.model.StartEventResponse;
import uk.gov.hmcts.reform.idam.client.IdamClient;
import uk.gov.hmcts.reform.pcs.ccd.domain.GeneralApplication;
import uk.gov.hmcts.reform.pcs.ccd.entity.GenApplication;
import uk.gov.hmcts.reform.pcs.ccd.repository.GeneralApplicationRepository;
import uk.gov.hmcts.reform.pcs.idam.IdamService;

import java.util.List;
import java.util.UUID;


@Service
public class GeneralApplicationService {

    @Autowired
    private IdamClient idamClient;

    private static final String JURISDICTION = "PCS";
    private static final String CASE_TYPE = "GA";
    private final CoreCaseDataApi coreCaseDataApi;

    private final GeneralApplicationRepository generalApplicationRepository;
    private final ModelMapper modelMapper;
    private final AuthTokenGenerator serviceAuthTokenGenerator;
    private final IdamService idamService;

    public GeneralApplicationService(CoreCaseDataApi coreCaseDataApi, GeneralApplicationRepository generalApplicationRepository,
                                     ModelMapper modelMapper,
                                     AuthTokenGenerator serviceAuthTokenGenerator,
                                     IdamService idamService) {
        this.coreCaseDataApi = coreCaseDataApi;

        this.generalApplicationRepository = generalApplicationRepository;
        this.modelMapper = modelMapper;
        this.serviceAuthTokenGenerator = serviceAuthTokenGenerator;
        this.idamService = idamService;
    }

    public void deleteDraft(UUID gaId) {
        generalApplicationRepository.deleteById(gaId);
    }

    public List<GenApplication> findByParentCase(long parentCaseId) {
        return generalApplicationRepository.findByPcsCase_CcdCaseReference(parentCaseId); //pcs repo?
    }

    public GenApplication convertToGAEntity(GeneralApplication gaCase) {
        return modelMapper.map(gaCase, GenApplication.class);
    }

    public GeneralApplication convertToGA(GenApplication gaCase) {
        return modelMapper.map(gaCase, GeneralApplication.class);
    }

    public String createGeneralApplicationInCCD(GeneralApplication gaData, String eventId) {
        //String userAuthToken = idamService.getSystemUserAuthorisation();
        //String serviceToken = serviceAuthTokenGenerator.generate();
        //String userId = idamService.validateAuthToken(userAuthToken).getUserDetails().getUid();

        String userAuthToken = idamClient.getAccessToken("caseworker@pcs.com", "password");
        String serviceToken = "Bearer eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJjY2RfZ3ciLCJpYXQiOjE2" +
            "ODAwMDAwMDB9.4QdWwz5ZQ5p5v3cJXkQ4lQwQkR2o9Yw5Qw8yQw8yQw8";

        String userId = idamClient.getUserInfo(userAuthToken).getUid();

        // Get event token
        StartEventResponse startEventResponse = coreCaseDataApi.startCase(
            userAuthToken,
            serviceToken,
            CASE_TYPE,
            eventId
        );

        // 2. Submit event
        CaseDataContent caseDataContent = CaseDataContent.builder()
            .eventToken(startEventResponse.getToken())
            .event(Event.builder()
                       .id(eventId)
                       .build())
            .data(gaData)
            .build();
        //submitcas
        CaseDetails createdCase = coreCaseDataApi.submitCaseCreation(
            userAuthToken,
            serviceToken,
            CASE_TYPE,
            caseDataContent
        );
        return createdCase.getId().toString();
    }

}
