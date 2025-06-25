package uk.gov.hmcts.reform.pcs.ccd;

import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.DecentralisedCaseRepository;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.entity.GenApplication;
import uk.gov.hmcts.reform.pcs.ccd.entity.PCS;
import uk.gov.hmcts.reform.pcs.ccd.repository.GeneralApplicationRepository;
import uk.gov.hmcts.reform.pcs.ccd.repository.PCSCaseRepository;
import uk.gov.hmcts.reform.pcs.ccd.service.GeneralApplicationService;
import uk.gov.hmcts.reform.pcs.ccd.service.PCSCaseService;

import java.util.List;

@Component
public class CCDCaseRepository extends DecentralisedCaseRepository<PCSCase> {

    private final PCSCaseRepository pcsCaseRepository;
    private final GeneralApplicationRepository generalApplicationRepository;
    private final GeneralApplicationService generalApplicationService;
    private final PCSCaseService pcsCaseService;

    public CCDCaseRepository(PCSCaseRepository pcsCaseRepository,
                          GeneralApplicationRepository generalApplicationRepository,
                          GeneralApplicationService generalApplicationService,
                          PCSCaseService pcsCaseService) {
        this.pcsCaseRepository = pcsCaseRepository;
        this.generalApplicationRepository = generalApplicationRepository;
        this.generalApplicationService = generalApplicationService;
        this.pcsCaseService = pcsCaseService;
    }

    @Override
    public PCSCase getCase(long caseRef) {
        PCS pcsCase = loadCaseData(caseRef);
        return pcsCaseService.convertToPCSCase(pcsCase);
    }

    private PCS loadCaseData(long caseRef) {
        return pcsCaseRepository.findByCcdCaseReference(caseRef)
                .orElseThrow(() -> new IllegalStateException("Parent case not found"));
    }

    private static void populateGALists(PCS pcsCase, List<GenApplication> applications) {
        // maybe extra logic is needed
        pcsCase.getGeneralApplications().clear();
        pcsCase.getGeneralApplications().addAll(applications);
    }



}
