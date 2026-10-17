package uk.gov.hmcts.reform.pcs.enforcement.view;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.view.CaseViewContributor;

@Component
@AllArgsConstructor
public class EnforcementCaseViewContributor implements CaseViewContributor {

    private final EnforcementOrderMediator enforcementOrderMediator;
    private final EnforcementOrderView enforcementOrderView;

    @Override
    public void contribute(PcsCaseEntity pcsCaseEntity, PCSCase pcsCase, long caseReference, State state) {
        enforcementOrderMediator.handleEnforcementRequirements(pcsCaseEntity, pcsCase);
        enforcementOrderView.setCaseFields(pcsCase, caseReference, state);
        enforcementOrderView.setEnforcementStatusFields(pcsCaseEntity, pcsCase);
    }

}
