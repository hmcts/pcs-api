package uk.gov.hmcts.reform.pcs.ccd.page.enforcetheorder.warrantofrestitution;

import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.page.enforcetheorder.ShowConditionsEnforcementType;

public class PeopleWhoWillBeEvictedWarrantRestitutionPage implements CcdPageConfiguration {

    @Override
    public void addTo(PageBuilder pageBuilder) {
        pageBuilder
            .page("peopleWhoWillBeEvictedWarrantRestitution")
            .pageLabel("The people who will be evicted (placeholder)")
            .showCondition(ShowConditionsEnforcementType.WARRANT_OF_RESTITUTION_FLOW);
    }
}
