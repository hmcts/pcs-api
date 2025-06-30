package uk.gov.hmcts.reform.pcs.ccd.service;

import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.ccd.client.model.CaseDataContent;
import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;
import uk.gov.hmcts.reform.ccd.client.model.CaseResource;
import uk.gov.hmcts.reform.ccd.client.model.Event;
import uk.gov.hmcts.reform.ccd.client.model.StartEventResponse;
import uk.gov.hmcts.reform.pcs.ccd.domain.GACase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.entity.GA;
import uk.gov.hmcts.reform.pcs.ccd.repository.GeneralApplicationRepository;

import java.util.List;


@Service
public class GeneralApplicationService {

    private static final String CASE_TYPE = "GA";

    private final CoreCaseDataService coreCaseDataService;
    private final GeneralApplicationRepository genAppRepository;
    private final ModelMapper modelMapper;

    public GeneralApplicationService(CoreCaseDataService coreCaseDataService,
                                     GeneralApplicationRepository genAppRepository,
                                     ModelMapper modelMapper) {
        this.coreCaseDataService = coreCaseDataService;
        this.genAppRepository = genAppRepository;
        this.modelMapper = modelMapper;
    }

    public List<GA> findByParentCase(Long parentCaseId) {
        return genAppRepository.findByPcsCase_CaseReference(parentCaseId); //pcs repo?
    }

    public GA findByCaseReference(Long gaCaseReference) {
        return genAppRepository.findByCaseReference(gaCaseReference)
            .orElseThrow(() -> new IllegalStateException("General Application not found"));
    }

    public GA convertToGAEntity(GACase gaCase) {
        return modelMapper.map(gaCase, GA.class);
    }

    public GACase convertToGA(GA gaCase) {
        return modelMapper.map(gaCase, GACase.class);
    }

    public void updateGA(Long caseRef) {
        genAppRepository.findByCaseReference(caseRef)
            .ifPresent(ga -> {
                ga.setStatus(State.Withdrawn);
                genAppRepository.save(ga);
            });
    }

    public CaseDetails getCase(String caseId) {
        return coreCaseDataService.getCase(caseId);
    }

    public Long createGeneralApplicationInCCD(GACase gaData, String eventId) {

        StartEventResponse startEventResponse = coreCaseDataService.startCase(
            CASE_TYPE,
            eventId
        );

        CaseDataContent caseDataContent = CaseDataContent.builder()
            .eventToken(startEventResponse.getToken())
            .event(Event.builder()
                       .id(eventId)
                       .build())
            .data(gaData)
            .build();

        CaseDetails createdCase = coreCaseDataService.submitCaseCreation(
            CASE_TYPE,
            caseDataContent
        );
        return createdCase.getId();
    }

    public CaseResource updateGeneralApplicationInCCD(String caseId, String eventId, GACase existingCase) {

        StartEventResponse eventToken = coreCaseDataService.startEvent(
            caseId,
            eventId
        );
        CaseDataContent caseDataContent = CaseDataContent.builder()
            .eventToken(eventToken.getToken())
            .event(Event.builder()
                       .id(eventId)
                       .build())
            .data(existingCase)
            .build();

        CaseResource updated = coreCaseDataService.submitEvent(
            caseId,
            caseDataContent
        );
        return updated;
    }


}
