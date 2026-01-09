package uk.gov.hmcts.reform.pcs.ccd.page.enforcetheorder.warrant;

import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.page.CommonPageContent;

public class StatementOfTruthPlaceHolder implements CcdPageConfiguration {

    @Override
    public void addTo(PageBuilder pageBuilder) {
        pageBuilder
                .page("statementOfTruthPlaceHolder")
                .pageLabel("Statement of truth (place holder)")
                .showCondition("isSuspendedOrder=\"YES\" AND selectEnforcementType=\"WARRANT\"")
                .label("statementOfTruthPlaceHolder-content", "---")
                .label("statementOfTruthPlaceHolder-saveAndReturn", CommonPageContent.SAVE_AND_RETURN);
    }
}

