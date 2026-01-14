package uk.gov.hmcts.reform.pcs.ccd.page.enforcetheorder.warrant;

import uk.gov.hmcts.ccd.sdk.api.CaseDetails;
import uk.gov.hmcts.ccd.sdk.api.callback.AboutToStartOrSubmitResponse;
import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.EnforcementOrder;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.warrant.WarrantDetails;
import uk.gov.hmcts.reform.pcs.ccd.type.DynamicMultiSelectStringList;

import java.util.ArrayList;
import java.util.List;

import static uk.gov.hmcts.reform.pcs.ccd.ShowConditions.NEVER_SHOW;
import static uk.gov.hmcts.reform.pcs.ccd.page.CommonPageContent.SAVE_AND_RETURN;

/**
 * Page for selecting specific defendants to evict.
 */
public class PeopleYouWantToEvictPage implements CcdPageConfiguration {

    @Override
    public void addTo(PageBuilder pageBuilder) {
        pageBuilder
            .page("peopleYouWantToEvict", this::midEvent)
            .pageLabel("The people you want to evict")
            .showCondition("warrantShowPeopleYouWantToEvictPage=\"Yes\" AND selectEnforcementType=\"WARRANT\"")
            .complex(PCSCase::getEnforcementOrder)
            .complex(EnforcementOrder::getWarrantDetails)
            .readonly(WarrantDetails::getShowPeopleYouWantToEvictPage, NEVER_SHOW)
            .done()
            .label("peopleYouWantToEvict-line-separator", "---")
            .complex(EnforcementOrder::getWarrantDetails)
            .mandatory(WarrantDetails::getSelectedDefendants)
            .done()
            .label("peopleYouWantToEvict-save-and-return", SAVE_AND_RETURN);
    }

    private AboutToStartOrSubmitResponse<PCSCase, State> midEvent(
        CaseDetails<PCSCase, State> details,
        CaseDetails<PCSCase, State> before) {
        
        PCSCase caseData = details.getData();
        List<String> errors = new ArrayList<>();
        
        EnforcementOrder enforcementOrder = caseData.getEnforcementOrder();
        DynamicMultiSelectStringList selectedDefendants = enforcementOrder.getWarrantDetails().getSelectedDefendants();
        
        // Validate that at least one defendant is selected
        if (selectedDefendants == null 
            || selectedDefendants.getValue() == null 
            || selectedDefendants.getValue().isEmpty()) {
            errors.add("Please select at least one person you want to evict");
        }
        
        if (!errors.isEmpty()) {
            return AboutToStartOrSubmitResponse.<PCSCase, State>builder()
                .data(caseData)
                .errors(errors)
                .build();
        }
        
        return AboutToStartOrSubmitResponse.<PCSCase, State>builder()
            .data(caseData)
            .build();
    }
}

