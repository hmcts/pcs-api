package uk.gov.hmcts.reform.pcs.ccd.page.createpossessionclaim;

import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;

public class RentArrears implements CcdPageConfiguration {

    @Override
    public void addTo(PageBuilder pageBuilder) {
        pageBuilder
                .page("rentArrears")
                .pageLabel("Details of rent arrears")
                .showCondition("rentFrequency=\"OTHER\"")

                // ---------- Rent statement guidance ----------
                .label("rentArrears-rentStatement-separator", "---")
                .label("rentArrears-rentStatement-heading",
                        """
                        <h2 class="govuk-heading-m">Rent statement</h2>
                        """)
                .label("rentArrears-rentStatement-help",
                        """
                        <p class="govuk-body govuk-!-margin-bottom-2"><strong>Upload the rent statement</strong></p>

                        <p class="govuk-body">The rent statement must show:</p>
                        <ul class="govuk-list govuk-list--bullet">
                          <li>every date when a payment was supposed to be made</li>
                          <li>the amount that was due on each of those dates</li>
                          <li>the actual payments that were made, and when they were made</li>
                          <li>the total rent arrears</li>
                        </ul>

                        <p class="govuk-body">It must cover the time period of either:</p>
                        <ul class="govuk-list govuk-list--bullet">
                          <li>from the first date the defendants missed a payment, or</li>
                          <li>the last two years of payments, if the first date of their missed payment was more
                          than two years ago</li>
                        </ul>
                        """)
                .optional(PCSCase::getRentStatementDocuments)

                // ---------- Total arrears ----------
                .label("rentArrears-totalArrears-separator", "---")
                .label("rentArrears-totalArrears-heading",
                        """
                        <h2 class="govuk-heading-m govuk-!-margin-bottom-0">Rent arrears</h2>
                        <h3 class="govuk-heading-s govuk-!-margin-top-0 govuk-!-margin-bottom-0">
                        How much are the total rent arrears as shown on the rent statement?</h3>
                        """)
                .mandatory(PCSCase::getTotalRentArrears)

                // ---------- Third-party payments ----------
                .label("rentArrears-thirdPartyPayments-separator", "---")
                .mandatory(PCSCase::getThirdPartyPayments)

                .mandatory(PCSCase::getThirdPartyPaymentSources, "thirdPartyPayments=\"YES\"")

                // "Other" free text is mandatory when OTHER is selected
                .mandatory(PCSCase::getThirdPartyPaymentSourceOther,
                        "thirdPartyPayments=\"YES\" AND thirdPartyPaymentSources CONTAINS \"OTHER\"");
    }
}
