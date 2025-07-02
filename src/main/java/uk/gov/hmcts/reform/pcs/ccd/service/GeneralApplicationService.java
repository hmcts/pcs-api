package uk.gov.hmcts.reform.pcs.ccd.service;

import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.sdk.type.CaseLink;
import uk.gov.hmcts.reform.ccd.client.model.CaseDataContent;
import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;
import uk.gov.hmcts.reform.ccd.client.model.CaseResource;
import uk.gov.hmcts.reform.ccd.client.model.Event;
import uk.gov.hmcts.reform.ccd.client.model.StartEventResponse;
import uk.gov.hmcts.reform.pcs.ccd.GeneralApplicationCaseType;
import uk.gov.hmcts.reform.pcs.ccd.domain.GACase;
import uk.gov.hmcts.reform.pcs.ccd.entity.GACaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.repository.GeneralApplicationRepository;


@Service
public class GeneralApplicationService {

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

    public GACaseEntity findByCaseReference(Long gaCaseReference) {
        return genAppRepository.findByCaseReference(gaCaseReference)
                .orElseThrow(() -> new IllegalStateException("General Application not found"));
    }

    public GACaseEntity convertToGAEntity(GACase gaCase) {
        return modelMapper.map(gaCase, GACaseEntity.class);
    }

    public GACase convertToGA(GACaseEntity gaCaseEntity) {
        GACase gaCase = modelMapper.map(gaCaseEntity, GACase.class);
        CaseLink caseLink = CaseLink.builder().caseType("PCS")
                .caseReference(gaCaseEntity.getParentCaseReference().toString()).build();
        gaCase.setCaseLink(caseLink);
        return gaCase;
    }

    public CaseDetails getCase(String caseId) {
        return coreCaseDataService.getCase(caseId);
    }

    public Long createGeneralApplicationInCCD(GACase gaData, String eventId) {

        StartEventResponse startEventResponse = coreCaseDataService.startCase(
                GeneralApplicationCaseType.CASE_TYPE_ID,
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
                GeneralApplicationCaseType.CASE_TYPE_ID,
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

    public void deleteGenApp(Long caseRef) {
        genAppRepository.deleteByCaseReference(caseRef);
    }

    public void saveGaApp(GACaseEntity gaEntity) {
        genAppRepository.save(gaEntity);
    }
}
