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
    private final BailiffNoteView bailiffNoteView;
    private final RiskLevelView riskLevelView;
    private final EvictionScheduleView evictionScheduleView;
    private final WarrantReturnView warrantReturnView;
    private final EnforcementStatusTabView enforcementStatusTabView;
    private final RiskAssessmentTabView riskAssessmentTabView;
    private final EnforcementDocumentVisibility enforcementDocumentVisibility;

    @Override
    public void contribute(PcsCaseEntity pcsCaseEntity, PCSCase pcsCase, long caseReference, State state) {
        enforcementOrderMediator.handleEnforcementRequirements(pcsCaseEntity, pcsCase);
        enforcementOrderView.setCaseFields(pcsCase, caseReference, state);
        enforcementOrderView.setEnforcementStatusFields(pcsCaseEntity, pcsCase);
        bailiffNoteView.setBailiffNotes(pcsCaseEntity, pcsCase);
        riskLevelView.setRiskAssessmentFields(pcsCaseEntity, pcsCase);
        evictionScheduleView.setEvictionScheduleFields(pcsCaseEntity, pcsCase);
        warrantReturnView.setLatestWarrantReturn(pcsCaseEntity, pcsCase);
        enforcementStatusTabView.setEnforcementStatusTab(pcsCaseEntity, pcsCase);
        riskAssessmentTabView.setRiskAssessmentTab(pcsCaseEntity, pcsCase, caseReference);
        enforcementDocumentVisibility.hideFromDefendants(pcsCase, caseReference);
    }

}
