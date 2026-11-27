package uk.gov.hmcts.reform.pcs.ccd.page.enforcetheorder.writ;

import uk.gov.hmcts.reform.pcs.ccd.ShowConditions;
import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.page.enforcetheorder.ShowConditionsEnforcementType;

public class UploadStatementWritPage implements CcdPageConfiguration {

    @Override
    public void addTo(PageBuilder pageBuilder) {
        pageBuilder
            .page("uploadStatementWritPage")
            .pageLabel("Upload statement of payments due " + "and made under the judgment or order")
            .label("uploadStatementWritPage-line-separator", "---")
            .showCondition(ShowConditions.and(ShowConditionsEnforcementType.WRIT_FLOW,
                                              ShowConditionsEnforcementType.SUSPENDED_ORDER,
                                              "writDefendantMissedPayments=\"YES\""));
    }
}
