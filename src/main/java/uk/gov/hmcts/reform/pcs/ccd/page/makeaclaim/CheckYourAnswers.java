package uk.gov.hmcts.reform.pcs.ccd.page.makeaclaim;

import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;

public class CheckYourAnswers implements CcdPageConfiguration {

    @Override
    public void addTo(PageBuilder pageBuilder) {
        pageBuilder
            .page("checkYourAnswers")
            .pageLabel("Check your answers")
            .showCondition("completionNextStep=\"SAVE_IT_FOR_LATER\"")

            // ---------- Horizontal separator ----------
            .label("check-your-answers-separator", "---")
            .label(
                "check-your-answers-content",
                """
                <h2 class="govuk-heading-l">Check your answers</h2>
                <p class="govuk-body govuk-!-font-size-19">
                  This is a placeholder page for the check your answers functionality.
                </p>
                <p class="govuk-body govuk-!-font-size-19">
                  In the full implementation, this page would contain:
                </p>
                <ul class="govuk-list govuk-list--bullet">
                  <li class="govuk-!-font-size-19">
                    Summary of all claim details entered
                  </li>
                  <li class="govuk-!-font-size-19">
                    Defendant information review
                  </li>
                  <li class="govuk-!-font-size-19">
                    Grounds for possession summary
                  </li>
                  <li class="govuk-!-font-size-19">
                    Rent arrears details
                  </li>
                  <li class="govuk-!-font-size-19">
                    Document uploads review
                  </li>
                  <li class="govuk-!-font-size-19">
                    Edit links for each section
                  </li>
                  <li class="govuk-!-font-size-19">
                    Save as draft functionality
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
