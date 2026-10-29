package uk.gov.hmcts.reform.pcs.ccd.page.enforcetheorder.warrant;

import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.page.enforcetheorder.ShowConditionsEnforcementType;

public class UploadStatementPage implements CcdPageConfiguration {

    @Override
    public void addTo(PageBuilder pageBuilder) {
        pageBuilder
            .page("uploadStatementPage")
            .pageLabel(
                "Upload statement of payments due "
                    + "and made under the judgment or order"
            )
            .showCondition(
                ShowConditionsEnforcementType.WARRANT_FLOW
                    + " AND warrantDefendantMissedPayments=\"YES\""
            );
    }


}
