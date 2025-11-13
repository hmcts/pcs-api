package uk.gov.hmcts.reform.pcs.ccd.page.makeaclaim;

import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.page.CommonPageContent;

public class StatementOfTruth implements CcdPageConfiguration {

    @Override
    public void addTo(PageBuilder pageBuilder) {
        pageBuilder
            .page("statementOfTruth")
            .pageLabel("Statement of truth")
            .showCondition("completionNextStep=\"SUBMIT_AND_PAY_NOW\"")

            // ---------- Horizontal separator ----------
            .label("statementOfTruth-separator", "---")
            .label(
                "statementOfTruth-content",
                """
                <h2 class="govuk-heading-l">Statement of truth</h2>
                <p class="govuk-body govuk-!-font-size-19">
                  This is a placeholder page for the statement of truth functionality.
                </p>
                """
            )
            .label("statementOfTruth-saveAndReturn", CommonPageContent.SAVE_AND_RETURN);
    }
}
