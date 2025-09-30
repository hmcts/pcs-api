package uk.gov.hmcts.reform.pcs.ccd.page.makeaclaim;

import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;

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
                <p class="govuk-body govuk-!-font-size-19">
                  In the full implementation, this page would contain:
                </p>
                <ul class="govuk-list govuk-list--bullet">
                  <li class="govuk-!-font-size-19">
                    A statement of truth declaration
                  </li>
                  <li class="govuk-!-font-size-19">
                    A checkbox to confirm understanding
                  </li>
                  <li class="govuk-!-font-size-19">
                    Digital signature capture
                  </li>
                  <li class="govuk-!-font-size-19">
                    Proceed to payment functionality
                  </li>
                </ul>
                <div class="govuk-warning-text">
                  <span class="govuk-warning-text__icon" aria-hidden="true">!</span>
                  <strong class="govuk-warning-text__text">
                    <span class="govuk-warning-text__assistive">Warning</span>
                    This is a placeholder page - functionality not yet implemented.
                  </strong>
                </div>
                """
            );
    }
}
