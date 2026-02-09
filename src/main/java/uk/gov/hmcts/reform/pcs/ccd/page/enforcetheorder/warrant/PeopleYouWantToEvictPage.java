package uk.gov.hmcts.reform.pcs.ccd.page.enforcetheorder.warrant;

import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.EnforcementOrder;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.warrant.RawWarrantDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.warrant.WarrantDetails;
import uk.gov.hmcts.reform.pcs.ccd.page.enforcetheorder.ShowConditionsWarrantOrWrit;

import static uk.gov.hmcts.reform.pcs.ccd.ShowConditions.NEVER_SHOW;
import static uk.gov.hmcts.reform.pcs.ccd.page.CommonPageContent.SAVE_AND_RETURN;

/**
 * Page for selecting specific defendants to evict.
 */
public class PeopleYouWantToEvictPage implements CcdPageConfiguration {

    @Override
    public void addTo(PageBuilder pageBuilder) {
        pageBuilder
            .page("peopleYouWantToEvict")
            .pageLabel("The people you want to evict")
            .showCondition(ShowConditionsWarrantOrWrit.WARRANT_FLOW
                + " AND warrantShowPeopleYouWantToEvictPage=\"Yes\"")
            .complex(PCSCase::getEnforcementOrder)
            .complex(EnforcementOrder::getWarrantDetails)
            .readonly(WarrantDetails::getShowPeopleYouWantToEvictPage, NEVER_SHOW)
            .done()
            .label("peopleYouWantToEvict-line-separator", "---")
            .complex(EnforcementOrder::getRawWarrantDetails)
            .mandatory(RawWarrantDetails::getSelectedDefendants)
            .done()
            .done()
            .label("peopleYouWantToEvict-save-and-return", SAVE_AND_RETURN);
    }

}

