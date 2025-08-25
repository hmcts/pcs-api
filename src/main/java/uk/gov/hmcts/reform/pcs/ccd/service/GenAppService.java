package uk.gov.hmcts.reform.pcs.ccd.service;


import lombok.AllArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.pcs.ccd.domain.GenApp;
import uk.gov.hmcts.reform.pcs.ccd.domain.GenAppEvent;
import uk.gov.hmcts.reform.pcs.ccd.domain.GenAppState;
import uk.gov.hmcts.reform.pcs.ccd.entity.GenAppEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.repository.GenAppRepository;
import uk.gov.hmcts.reform.pcs.exception.GenAppNotFoundException;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@AllArgsConstructor
public class GenAppService {

    private final GenAppRepository genAppRepository;
    private final PcsCaseService pcsCaseService;
    private final GenAppEventService genAppEventService;
    private final ModelMapper modelMapper;

    public GenApp getGenApp(UUID genAppId) {
        GenAppEntity genAppEntity = getGenAppEntity(genAppId);

        return modelMapper.map(genAppEntity, GenApp.class);
    }

    public UUID createGenApp(long caseReference,
                             String genAppDetails) {

        PcsCaseEntity pcsCaseEntity = pcsCaseService.getCaseByCaseReference(caseReference);

        GenAppEntity genAppEntity = GenAppEntity.builder()
            .state(GenAppState.AWAITING_SUBMISSION_TO_HMCTS)
            .created(Instant.now())
            .summary(genAppDetails)
            .build();

        pcsCaseEntity.addGenApp(genAppEntity);

        genAppRepository.save(genAppEntity);

        return genAppEntity.getId();
    }

    public void setGenAppState(UUID genAppId, GenAppState genAppState) {
        GenAppEntity genAppEntity = getGenAppEntity(genAppId);

        genAppEntity.setState(genAppState);

        genAppRepository.save(genAppEntity);
    }

    public List<GenAppEvent> getApplicableEvents(UUID genAppId) {
        GenAppState genAppState = getGenAppEntity(genAppId).getState();
        if (genAppState == null) {
            return List.of();
        }
        return getApplicableEvents(genAppState);
    }

    public List<GenAppEvent> getApplicableEvents(GenAppState genAppState) {
        if (genAppState == null) {
            return List.of();
        }

        return genAppEventService.getAllEvents().stream()
            .filter(event -> event.isApplicableFor(genAppState))
            .toList();
    }

    private GenAppEntity getGenAppEntity(UUID genAppId) {
        return genAppRepository.findById(genAppId)
            .orElseThrow(() -> new GenAppNotFoundException(genAppId));
    }

}
