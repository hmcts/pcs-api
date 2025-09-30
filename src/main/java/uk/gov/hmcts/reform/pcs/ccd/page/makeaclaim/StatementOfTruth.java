package uk.gov.hmcts.reform.pcs.ccd.page.makeaclaim;

import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;

public class StatementOfTruth implements CcdPageConfiguration {

    @Override
    public void addTo(PageBuilder pageBuilder) {
        pageBuilder
            .page("statementOfTruth")
            .pageLabel("Statement of truth")
            .showCondition("completionNextStep=\"SUBMIT_AND_PAY_NOW\"")

            // ---------- Horizontal separator ----------
            .label("statement-of-truth-separator", "---")
            .label(
                "statement-of-truth-content",
                """
                <h2 class="govuk-heading-l">Statement of truth</h2>
                <p class="govuk-body govuk-!-font-size-19">
                  This is a placeholder page for the statement of truth functionality.
                </p>
                """
            );
    }
}
