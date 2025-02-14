package uk.gov.hmcts.reform.pcs.ccd;

import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.CaseRepository;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;

@Component
public class PCSCaseRepository implements CaseRepository<PCSCase> {
    @Override
    public PCSCase getCase(long caseRef, PCSCase data, String roleAssignments) {
        return data;
    }
}
