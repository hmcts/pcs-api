package uk.gov.hmcts.reform.pcs.ccd;

import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.DecentralisedCaseRepository;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.entity.GenApplication;
import uk.gov.hmcts.reform.pcs.ccd.entity.PCS;
import uk.gov.hmcts.reform.pcs.ccd.renderer.GeneralApplicationRenderer;
import uk.gov.hmcts.reform.pcs.ccd.repository.GeneralApplicationRepository;
import uk.gov.hmcts.reform.pcs.ccd.repository.PCSCaseRepository;
import uk.gov.hmcts.reform.pcs.ccd.service.GeneralApplicationService;
import uk.gov.hmcts.reform.pcs.ccd.service.PCSCaseService;

import java.util.Optional;

@Component
public class CCDCaseRepository extends DecentralisedCaseRepository<Object> {

    private final PCSCaseRepository pcsCaseRepository;
    private final GeneralApplicationRepository generalApplicationRepository;
    private final GeneralApplicationService generalApplicationService;
    private final PCSCaseService pcsCaseService;
    private final GeneralApplicationRenderer genAppRenderer;

    public CCDCaseRepository(PCSCaseRepository pcsCaseRepository,
                             GeneralApplicationRepository generalApplicationRepository,
                             GeneralApplicationService generalApplicationService,
                             PCSCaseService pcsCaseService, GeneralApplicationRenderer genAppRenderer) {
        this.pcsCaseRepository = pcsCaseRepository;
        this.generalApplicationRepository = generalApplicationRepository;
        this.generalApplicationService = generalApplicationService;
        this.pcsCaseService = pcsCaseService;
        this.genAppRenderer = genAppRenderer;
    }

    @Override
    public Object getCase(long caseRef) {
        // Try PCS first
        Optional<PCS> pcs = pcsCaseRepository.findByCcdCaseReference(caseRef);
        if (pcs.isPresent()) {
            PCSCase pcsCase = pcsCaseService.convertToPCSCase(pcs.get());
            pcsCase.setGeneralApplicationsSummaryMarkdown(genAppRenderer.render(
                pcsCase.getGeneralApplications(),
                caseRef
            ));
            return pcsCase;
        }
        // Else try GA
        Optional<GenApplication> ga = generalApplicationRepository.findByApplicationId(String.valueOf(caseRef));
        if (ga.isPresent()) {
            return generalApplicationService.convertToGA(ga.get());
        }
        return pcs;
    }

}
