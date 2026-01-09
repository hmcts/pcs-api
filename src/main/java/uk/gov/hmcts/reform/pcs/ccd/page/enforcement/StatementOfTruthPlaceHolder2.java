package uk.gov.hmcts.reform.pcs.ccd.page.enforcement;

import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.page.CommonPageContent;

public class StatementOfTruthPlaceHolder2 implements CcdPageConfiguration {

    @Override
    public void addTo(PageBuilder pageBuilder) {
        pageBuilder
                .page("statementOfTruthPlaceHolder2")
                .pageLabel("Statement of truth (place holder2)")
                .showCondition("isSuspendedOrder=\"NO\"")
                .label("statementOfTruthPlaceHolder2-content", "---")
                .label("statementOfTruthPlaceHolder2-saveAndReturn", CommonPageContent.SAVE_AND_RETURN);
    }
}

