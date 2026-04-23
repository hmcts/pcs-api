package uk.gov.hmcts.reform.pcs.ccd.page.enforcetheorder.warrantofrestitution;

import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;

import static uk.gov.hmcts.reform.pcs.ccd.page.CommonPageContent.SAVE_AND_RETURN;
import static uk.gov.hmcts.reform.pcs.ccd.page.enforcetheorder.ShowConditionsEnforcementType.WARRANT_OF_RESTITUTION_FLOW;

public class PeopleYouWantToEvictWarrantRestPlaceholder implements CcdPageConfiguration {

    @Override
    public void addTo(PageBuilder pageBuilder) {
        pageBuilder
            .page("peopleYouWantToEvictWarrantRest")
            .pageLabel("The people you want to evict (placeholder)")
            .showCondition(WARRANT_OF_RESTITUTION_FLOW)
            .label("peopleYouWantToEvictWarrantRest-line-separator", "---")
            .label("peopleYouWantToEvictWarrantRest-save-and-return", SAVE_AND_RETURN);
    }
}
