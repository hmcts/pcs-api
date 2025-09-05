package uk.gov.hmcts.reform.pcs.ccd.page.createpossessionclaim;

import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;

public class RentArrears implements CcdPageConfiguration {

    @Override
    public void addTo(PageBuilder pageBuilder) {
        pageBuilder
                .page("rentArrears")
                .showCondition("rentFrequency=\"OTHER\"")
                .pageLabel("Details of rent arrears")

                // ---------- Rent statement guidance ----------
                .label("rentStatementHeading",
                        """
                        ---
                        <h2 class="govuk-heading-m">Rent statement</h2>
                        """)
                .label("rentStatementHelp",
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
                          <li>the last two years of payments, if the first date of their missed payment was more than
                          two years ago</li>
                        </ul>
                        """)
                // Upload rent statement (collection of Document)
                .optional(PCSCase::getRentStatementDocuments)

                // ---------- Total arrears ----------
                .label("rentArrearsHeading",
                        """
                        ---
                        <h2 class="govuk-heading-m">Rent arrears</h2>
                        <h3 class="govuk-heading-s">How much are the total rent arrears as shown on the rent statement?</h3>
                        """)
                .mandatory(PCSCase::getTotalRentArrears)

                // ---------- Third-party payments ----------
                .mandatory(PCSCase::getThirdPartyPayments)

                // Sources (select all that apply) + hint
                .label("thirdPartySourcesLegend",
                        """
                        <h3 class="govuk-heading-s govuk-!-margin-bottom-1">Where have the payments come from?</h3>
                        <div class="govuk-hint govuk-!-margin-bottom-2">Select all that apply</div>
                        """,
                        "thirdPartyPayments=\"YES\"")
                .optional(PCSCase::getThirdPartyPaymentSources, "thirdPartyPayments=\"YES\"")

                // "Other" free text is mandatory when OTHER is selected
                .mandatory(
                        PCSCase::getThirdPartyPaymentSourceOther,
                        "thirdPartyPayments=\"YES\" AND thirdPartyPaymentSources CONTAINS \"OTHER\""
                );
    }
}
