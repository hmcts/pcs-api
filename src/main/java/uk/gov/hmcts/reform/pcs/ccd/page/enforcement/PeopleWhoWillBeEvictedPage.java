package uk.gov.hmcts.reform.pcs.ccd.page.enforcement;

import uk.gov.hmcts.ccd.sdk.api.CaseDetails;
import uk.gov.hmcts.ccd.sdk.api.callback.AboutToStartOrSubmitResponse;
import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcement.EnforcementOrder;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcement.PeopleToEvict;

import java.util.ArrayList;
import java.util.List;

import static uk.gov.hmcts.reform.pcs.ccd.ShowConditions.NEVER_SHOW;
import static uk.gov.hmcts.reform.pcs.ccd.page.CommonPageContent.SAVE_AND_RETURN;

/**
 * Page for selecting whether to evict everyone or specific people.
 */
public class PeopleWhoWillBeEvictedPage implements CcdPageConfiguration {

    @Override
    public void addTo(PageBuilder pageBuilder) {
        pageBuilder
            .page("peopleWhoWillBeEvicted", this::midEvent)
            .pageLabel("The people who will be evicted")
            .showCondition("showPeopleWhoWillBeEvictedPage=\"YES\"")
            .complex(PCSCase::getEnforcementOrder)
            .readonly(EnforcementOrder::getShowPeopleWhoWillBeEvictedPage, NEVER_SHOW)
            .done()
            .label("peopleWhoWillBeEvicted-line-separator", "---")
            .complex(PCSCase::getEnforcementOrder)
            .complex(EnforcementOrder::getPeopleToEvict)
            .mandatory(PeopleToEvict::getEvictEveryone)
            .done()
            .label("peopleWhoWillBeEvicted-save-and-return", SAVE_AND_RETURN);
    }

    private AboutToStartOrSubmitResponse<PCSCase, State> midEvent(
        CaseDetails<PCSCase, State> details,
        CaseDetails<PCSCase, State> before) {
        
        PCSCase caseData = details.getData();
        List<String> errors = new ArrayList<>();
        
        // Validate that a selection has been made
        PeopleToEvict peopleToEvict = caseData.getEnforcementOrder().getPeopleToEvict();
        if (peopleToEvict.getEvictEveryone() == null) {
            errors.add("Please select whether you want to evict everyone or specific people");
        }
        
        if (!errors.isEmpty()) {
            return AboutToStartOrSubmitResponse.<PCSCase, State>builder()
                .data(caseData)
                .errors(errors)
                .build();
        }
        
        EnforcementOrder enforcementOrder = caseData.getEnforcementOrder();
        if (peopleToEvict.getEvictEveryone() == VerticalYesNo.NO) {
            // Navigate to PeopleYouWantToEvictPage
            enforcementOrder.setShowPeopleYouWantToEvictPage(VerticalYesNo.YES);
        } else if (peopleToEvict.getEvictEveryone() == VerticalYesNo.YES) {
            // Skip PeopleYouWantToEvictPage, go directly to LivingInThePropertyPage
            enforcementOrder.setShowPeopleYouWantToEvictPage(VerticalYesNo.NO);
        }
        
        return AboutToStartOrSubmitResponse.<PCSCase, State>builder()
            .data(caseData)
            .build();
    }
}

