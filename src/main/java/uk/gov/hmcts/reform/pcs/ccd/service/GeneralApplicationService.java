package uk.gov.hmcts.reform.pcs.ccd.service;

import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.pcs.ccd.domain.GeneralApplication;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.entity.GenApplication;
import uk.gov.hmcts.reform.pcs.ccd.entity.PCS;
import uk.gov.hmcts.reform.pcs.ccd.repository.GeneralApplicationRepository;
import uk.gov.hmcts.reform.pcs.ccd.repository.PCSCaseRepository;

import java.util.List;

@Service
public class GeneralApplicationService {
    private final GeneralApplicationRepository generalApplicationRepository;
    private final PCSCaseRepository pcsCaseRepository;
    private final PCSCaseService pcsCaseService;

    public GeneralApplicationService(GeneralApplicationRepository generalApplicationRepository,
                                     PCSCaseRepository pcsCaseRepository,
                                     PCSCaseService pcsCaseService) {
        this.generalApplicationRepository = generalApplicationRepository;
        this.pcsCaseRepository = pcsCaseRepository;
        this.pcsCaseService = pcsCaseService;
    }

    public void saveDraft(GenApplication ga) {
        ga.setStatus(State.Draft);
        generalApplicationRepository.save(ga);

    }

    public void deleteDraft(long gaId) {
        generalApplicationRepository.deleteById(gaId);
    }

    public List<GenApplication> findByParentCase(long parentCaseId) {
        return generalApplicationRepository.findByPcsCase_CcdCaseReference(parentCaseId);
    }

    private GenApplication convertToGAEntity(GeneralApplication gaCase) {
        if (gaCase == null) {
            return null;
        }
        GenApplication.GenApplicationBuilder builder = GenApplication.builder()
            .applicationId(gaCase.getApplicationId());
        if (gaCase.getStatus() != null) {
            builder.status(gaCase.getStatus());
        }

        if (gaCase.getParentCaseReference() != null) {
            PCS parentCase = pcsCaseRepository.findByCcdCaseReference(gaCase.getParentCaseReference());
            builder.pcsCase(parentCase);
        }

        if (gaCase.getAdjustment() != null) {
            builder.adjustment(gaCase.getAdjustment());
        }

        return builder.build();
    }


}
