package uk.gov.hmcts.reform.pcs.ccd;

import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.DecentralisedCaseRepository;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.repository.PcsCaseEntityRepository;
import uk.gov.hmcts.reform.pcs.exception.CaseNotFoundException;


/**
 * Invoked by CCD to load PCS cases under the decentralised model.
 */
@Component
public class CCDCaseRepository extends DecentralisedCaseRepository<PCSCase> {

    private final PcsCaseEntityRepository pcsCaseEntityRepository;

    public CCDCaseRepository(PcsCaseEntityRepository pcsCaseEntityRepository) {
        this.pcsCaseEntityRepository = pcsCaseEntityRepository;
    }

    /**
     * Invoked by CCD to load PCS cases by reference.
     * @param caseRef The case to load
     */
    @Override
    public PCSCase getCase(long caseRef, String roleAssignments) {
        PcsCaseEntity pcsCaseEntity = pcsCaseEntityRepository.findById(caseRef)
            .orElseThrow(() -> new CaseNotFoundException(caseRef));

        return PCSCase.builder()
            .applicantForename(pcsCaseEntity.getApplicantForename())
            .build();
    }

}
