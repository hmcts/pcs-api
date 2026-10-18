package uk.gov.hmcts.reform.pcs.enforcement.view;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.view.CaseViewContributor;
import uk.gov.hmcts.reform.pcs.enforcement.service.CurrentEnforcementService;
import uk.gov.hmcts.reform.pcs.service.FeatureFlag;
import uk.gov.hmcts.reform.pcs.service.FeatureToggleService;

@Component
@AllArgsConstructor
public class EnforcementCaseViewContributor implements CaseViewContributor {

    private final FeatureToggleService featureToggleService;
    private final EnforcementOrderMediator enforcementOrderMediator;
    private final EnforcementOrderView enforcementOrderView;
    private final BailiffNoteView bailiffNoteView;
    private final RiskLevelView riskLevelView;
    private final EvictionScheduleView evictionScheduleView;
    private final WarrantReturnView warrantReturnView;
    private final EnforcementStatusTabView enforcementStatusTabView;
    private final RiskAssessmentTabView riskAssessmentTabView;
    private final EnforcementEventAvailabilityView enforcementEventAvailabilityView;
    private final ClaimantEnforcementDocumentView claimantEnforcementDocumentView;
    private final DefendantEnforcementView defendantEnforcementView;

    /**
     * Every view below reads the claim's enforcements, so they are loaded once for all of them.
     *
     * <p>With the {@code enforcement-enabled} flag off no enforcement field is set. Enforcement documents already
     * in the case file are still restricted as usual, so turning the flag off never shows them to anyone new.
     */
    @Override
    public void contribute(PcsCaseEntity pcsCaseEntity, PCSCase pcsCase, long caseReference, State state) {
        CurrentEnforcementService.withEnforcementsLoadedOnce(() -> setFields(pcsCaseEntity, pcsCase, caseReference,
                                                                             state));
    }

    private void setFields(PcsCaseEntity pcsCaseEntity, PCSCase pcsCase, long caseReference, State state) {
        if (!featureToggleService.isEnabled(FeatureFlag.ENFORCEMENT)) {
            if (pcsCase != null) {
                pcsCase.setEnforcementOrder(null);
            }
            restrict(pcsCaseEntity, pcsCase, caseReference);
            return;
        }
        enforcementOrderMediator.handleEnforcementRequirements(pcsCaseEntity, pcsCase);
        enforcementOrderView.setCaseFields(pcsCase, caseReference, state);
        enforcementOrderView.setEnforcementStatusFields(pcsCaseEntity, pcsCase);
        bailiffNoteView.setBailiffNotes(pcsCaseEntity, pcsCase);
        riskLevelView.setRiskAssessmentFields(pcsCaseEntity, pcsCase);
        evictionScheduleView.setEvictionScheduleFields(pcsCaseEntity, pcsCase);
        warrantReturnView.setLatestWarrantReturn(pcsCaseEntity, pcsCase);
        enforcementStatusTabView.setEnforcementStatusTab(pcsCaseEntity, pcsCase);
        riskAssessmentTabView.setRiskAssessmentTab(pcsCaseEntity, pcsCase, caseReference);
        enforcementEventAvailabilityView.setEventAvailability(pcsCaseEntity, pcsCase, state);
        // Last, so that they restrict everything the views above set.
        restrict(pcsCaseEntity, pcsCase, caseReference);
    }

    private void restrict(PcsCaseEntity pcsCaseEntity, PCSCase pcsCase, long caseReference) {
        claimantEnforcementDocumentView.restrictForClaimants(pcsCaseEntity, pcsCase, caseReference);
        defendantEnforcementView.restrictForDefendants(pcsCaseEntity, pcsCase, caseReference);
    }

}
