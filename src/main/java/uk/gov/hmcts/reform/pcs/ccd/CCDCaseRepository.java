package uk.gov.hmcts.reform.pcs.ccd;

import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.DecentralisedCaseRepository;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.entity.GA;
import uk.gov.hmcts.reform.pcs.ccd.entity.PCS;
import uk.gov.hmcts.reform.pcs.ccd.renderer.GeneralApplicationRenderer;
import uk.gov.hmcts.reform.pcs.ccd.repository.GeneralApplicationRepository;
import uk.gov.hmcts.reform.pcs.ccd.repository.PcsCaseRepository;
import uk.gov.hmcts.reform.pcs.ccd.service.GeneralApplicationService;
import uk.gov.hmcts.reform.pcs.ccd.service.PCaseService;

import java.util.Optional;

@Component
public class CCDCaseRepository extends DecentralisedCaseRepository<Object> {

    private final PcsCaseRepository pcsCaseRepository;
    private final GeneralApplicationRepository generalApplicationRepository;
    private final GeneralApplicationService generalApplicationService;
    private final PCaseService pcsCaseService;
    private final GeneralApplicationRenderer genAppRenderer;

    public CCDCaseRepository(PcsCaseRepository pcsCaseRepository,
                             GeneralApplicationRepository generalApplicationRepository,
                             GeneralApplicationService generalApplicationService,
                             PCaseService pcsCaseService, GeneralApplicationRenderer genAppRenderer) {
        this.pcsCaseRepository = pcsCaseRepository;
        this.generalApplicationRepository = generalApplicationRepository;
        this.generalApplicationService = generalApplicationService;
        this.pcsCaseService = pcsCaseService;
        this.genAppRenderer = genAppRenderer;
    }

    @Override
    public Object getCase(long caseRef) {
        // Try PCS first
        Optional<PCS> pcs = pcsCaseRepository.findByCaseReference(caseRef);
        if (pcs.isPresent()) {
            PCSCase pcsCase = pcsCaseService.convertToPCSCase(pcs.get());
            pcsCase.setGeneralApplicationsSummaryMarkdown(genAppRenderer.render(
                pcsCase.getGeneralApplications(),
                caseRef
            ));
            return pcsCase;
        }
        // Else try GA
        Optional<GA> ga = generalApplicationRepository.findByCaseReference(caseRef);
        if (ga.isPresent()) {
            return generalApplicationService.convertToGA(ga.get());
        }
        return pcs;

    }

}
