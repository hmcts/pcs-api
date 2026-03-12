package uk.gov.hmcts.reform.pcs.ccd.page.enforcetheorder.warrantofrestitution;

import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;

import static uk.gov.hmcts.reform.pcs.ccd.page.CommonPageContent.SAVE_AND_RETURN;
import static uk.gov.hmcts.reform.pcs.ccd.page.enforcetheorder.ShowConditionsEnforcementType.WARRANT_OF_RESTITUTION_FLOW;

public class PeopleWhoWillBeEvictedWarrantRestPlaceholder implements CcdPageConfiguration {

    @Override
    public void addTo(PageBuilder pageBuilder) {
        pageBuilder
            .page("peopleWhoWillBeEvictedWarrantRestitution")
            .pageLabel("The people who will be evicted (placeholder)")
            .showCondition(WARRANT_OF_RESTITUTION_FLOW)
            .label("peopleWhoWillBeEvictedWarrantRestitution-line-separator", "---")
            .label("peopleWhoWillBeEvictedWarrantRestitution-save-and-return", SAVE_AND_RETURN);
    }
}
