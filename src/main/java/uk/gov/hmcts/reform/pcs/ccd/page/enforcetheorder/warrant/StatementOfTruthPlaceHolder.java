package uk.gov.hmcts.reform.pcs.ccd.page.enforcetheorder.warrant;

import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.page.CommonPageContent;
import uk.gov.hmcts.reform.pcs.ccd.page.enforcetheorder.ShowConditionsWarrantOrWrit;

public class StatementOfTruthPlaceHolder implements CcdPageConfiguration {

    @Override
    public void addTo(PageBuilder pageBuilder) {
        pageBuilder
                .page("statementOfTruthPlaceHolder")
                .pageLabel("Statement of truth (place holder)")
                .showCondition(ShowConditionsWarrantOrWrit.WARRANT_FLOW
                    + " AND warrantIsSuspendedOrder=\"YES\"")
                .label("statementOfTruthPlaceHolder-content", "---")
                .label("statementOfTruthPlaceHolder-saveAndReturn", CommonPageContent.SAVE_AND_RETURN);
    }
}

