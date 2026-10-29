package uk.gov.hmcts.reform.pcs.ccd.page.enforcetheorder.warrant;

import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.page.enforcetheorder.ShowConditionsEnforcementType;

import static uk.gov.hmcts.reform.pcs.ccd.page.CommonPageContent.SAVE_AND_RETURN;

public class UploadStatementPage implements CcdPageConfiguration {

    @Override
    public void addTo(PageBuilder pageBuilder) {
        pageBuilder
            .page("uploadStatementPage")
            .pageLabel("Upload statement of payments due " + "and made under the judgment or order")
            .label("uploadStatementPage-line-separator", "---")
            .showCondition(ShowConditionsEnforcementType.WARRANT_FLOW + " AND warrantDefendantMissedPayments=\"YES\"")
            .label("uploadStatementPage-save-and-return", SAVE_AND_RETURN);
    }


}
