package uk.gov.hmcts.reform.pcs.ccd.service;

import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.pcs.ccd.domain.GeneralApplication;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.entity.GenApplication;
import uk.gov.hmcts.reform.pcs.ccd.repository.GeneralApplicationRepository;
import uk.gov.hmcts.reform.pcs.ccd.repository.PCSCaseRepository;

import java.util.List;
import java.util.UUID;

@Service
public class GeneralApplicationService {
    private final GeneralApplicationRepository generalApplicationRepository;
    private final PCSCaseRepository pcsCaseRepository;
    private final PCSCaseService pcsCaseService;
    private final ModelMapper modelMapper;

    public GeneralApplicationService(GeneralApplicationRepository generalApplicationRepository,
                                     PCSCaseRepository pcsCaseRepository,
                                     PCSCaseService pcsCaseService, ModelMapper modelMapper) {
        this.generalApplicationRepository = generalApplicationRepository;
        this.pcsCaseRepository = pcsCaseRepository;
        this.pcsCaseService = pcsCaseService;
        this.modelMapper = modelMapper;
    }

    public void saveDraft(GenApplication ga) {
        ga.setStatus(State.Draft);
        generalApplicationRepository.save(ga);

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


}
