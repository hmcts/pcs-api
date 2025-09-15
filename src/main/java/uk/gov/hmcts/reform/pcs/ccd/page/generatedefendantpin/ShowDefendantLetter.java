package uk.gov.hmcts.reform.pcs.ccd.page.generatedefendantpin;

import uk.gov.hmcts.reform.pcs.ccd.ShowConditions;
import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;

public class ShowDefendantLetter implements CcdPageConfiguration {

    @Override
    public void addTo(PageBuilder pageBuilder) {
        pageBuilder
            .page("showDefendantLetter")
            .pageLabel("Defendant letter")
            .readonly(PCSCase::getLinkCode, ShowConditions.NEVER_SHOW, true)
            .readonly(PCSCase::getDefendant1, ShowConditions.NEVER_SHOW, true)
            .readonly(PCSCase::getFormattedPropertyAddress, ShowConditions.NEVER_SHOW, true)
            .label("showDefendantLetter-info", """
                ---
                Dear ${defendant1.firstName} ${defendant1.lastName},

                We are contacting you because a claim has been made against you with regards to
                the property at ${formattedPropertyAddress}

                ### How to view the claim online

                <ol class="govuk-list govuk-list--number">
                  <li>Go to: https://test.com</li>
                  <li>Enter the claim number: <span class="govuk-body govuk-!-font-weight-bold">${[CASE_REFERENCE]}</span></li>
                  <li>Enter the security code: <span class="govuk-body govuk-!-font-weight-bold">${linkCode}</span></li>
                </ol>

                <br>

                <p class="govuk-body govuk-!-font-size-19 govuk-!-font-weight-bold">HM Courts & Tribunals Service</p>

                <br>
                <br>
                """);
    }

}
