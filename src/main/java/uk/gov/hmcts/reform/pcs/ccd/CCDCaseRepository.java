package uk.gov.hmcts.reform.pcs.ccd;

import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.DecentralisedCaseRepository;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;


/**
 * Invoked by CCD to load PCS cases under the decentralised model.
 */
@Component
public class CCDCaseRepository extends DecentralisedCaseRepository<PCSCase> {

    /**
     * Invoked by CCD to load PCS cases by reference.
     * @param caseRef The case to load
     */
    @Override
    public PCSCase getCase(long caseRef, String roleAssignments) {
        return PCSCase.builder().build();
    }

}
