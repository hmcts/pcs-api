package uk.gov.hmcts.reform.pcs.ccd.page.enforcetheorder.warrant;

import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;

import static uk.gov.hmcts.reform.pcs.ccd.page.CommonPageContent.SAVE_AND_RETURN;

public class RepaymentsPage implements CcdPageConfiguration {

    @Override
    public void addTo(PageBuilder pageBuilder) {
        pageBuilder
                .page("repaymentsPage")
                .pageLabel("Repayments Page (place holder)")
                .showCondition("selectEnforcementType=\"WARRANT\"")
                .label("repaymentsPage-content", "---")
                .label("repaymentsPage-save-and-return", SAVE_AND_RETURN);
    }

}
