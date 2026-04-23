package uk.gov.hmcts.reform.pcs.ccd.page.enforcetheorder.warrant;

import uk.gov.hmcts.ccd.sdk.api.CaseDetails;
import uk.gov.hmcts.ccd.sdk.api.callback.AboutToStartOrSubmitResponse;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.EnforcementOrder;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.warrant.PeopleToEvict;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.warrant.WarrantDetails;
import uk.gov.hmcts.reform.pcs.ccd.page.enforcetheorder.ShowConditionsEnforcementType;

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
            .showCondition(ShowConditionsEnforcementType.WARRANT_FLOW
                + " AND warrantShowPeopleWhoWillBeEvictedPage=\"Yes\"")
            .complex(PCSCase::getEnforcementOrder)
            .complex(EnforcementOrder::getWarrantDetails)
            .readonly(WarrantDetails::getShowPeopleWhoWillBeEvictedPage, NEVER_SHOW)
            .done()
            .label("peopleWhoWillBeEvicted-line-separator", "---")
            .complex(EnforcementOrder::getWarrantDetails)
            .complex(WarrantDetails::getPeopleToEvict)
            .mandatory(PeopleToEvict::getEvictEveryone)
            .done()
            .label("peopleWhoWillBeEvicted-save-and-return", SAVE_AND_RETURN);
    }

    private AboutToStartOrSubmitResponse<PCSCase, State> midEvent(
        CaseDetails<PCSCase, State> details,
        CaseDetails<PCSCase, State> before) {

        PCSCase caseData = details.getData();

        WarrantDetails warrantDetails = caseData.getEnforcementOrder().getWarrantDetails();
        PeopleToEvict peopleToEvict = warrantDetails.getPeopleToEvict();
        if (peopleToEvict.getEvictEveryone() == VerticalYesNo.NO) {
            // Navigate to PeopleYouWantToEvictPage
            warrantDetails.setShowPeopleYouWantToEvictPage(YesOrNo.YES);
        } else if (peopleToEvict.getEvictEveryone() == VerticalYesNo.YES) {
            // Skip PeopleYouWantToEvictPage, go directly to LivingInThePropertyPage
            warrantDetails.setShowPeopleYouWantToEvictPage(YesOrNo.NO);
        }

        return AboutToStartOrSubmitResponse.<PCSCase, State>builder()
            .data(caseData)
            .build();
    }
}

