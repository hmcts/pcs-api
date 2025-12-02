package uk.gov.hmcts.reform.pcs.ccd.page.enforcement;

import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.page.CommonPageContent;

public class RepaymentsPlaceHolder implements CcdPageConfiguration {

    @Override
    public void addTo(PageBuilder pageBuilder) {
        pageBuilder
                .page("repaymentsPlaceHolder")
                .pageLabel("Repayments (place holder)")
                .label("repaymentsPlaceHolder-content", "---")
                .label("repaymentsPlaceHolder-saveAndReturn", CommonPageContent.SAVE_AND_RETURN);
    }
}

