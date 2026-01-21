package uk.gov.hmcts.reform.pcs.ccd.page.enforcetheorder.warrant;

import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.page.CommonPageContent;
import uk.gov.hmcts.reform.pcs.ccd.page.enforcetheorder.ShowConditionsWarrantOrWrit;

public class StatementOfTruthPlaceHolder2 implements CcdPageConfiguration {

    @Override
    public void addTo(PageBuilder pageBuilder) {
        pageBuilder
                .page("statementOfTruthPlaceHolder2")
                .pageLabel("Statement of truth (place holder2)")
                .showCondition(ShowConditionsWarrantOrWrit.WARRANT_FLOW
                    + " AND warrantIsSuspendedOrder=\"NO\"")
                .label("statementOfTruthPlaceHolder2-content", "---")
                .label("statementOfTruthPlaceHolder2-saveAndReturn", CommonPageContent.SAVE_AND_RETURN);
    }
}

