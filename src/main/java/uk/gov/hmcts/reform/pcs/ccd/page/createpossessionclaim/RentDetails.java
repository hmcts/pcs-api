package uk.gov.hmcts.reform.pcs.ccd.page.createpossessionclaim;

import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;

/**
 * Page configuration for the Rent Details section.
 * Allows claimants to enter rent amount and payment frequency details.
 */
public class RentDetails implements CcdPageConfiguration {

    @Override
    public void addTo(PageBuilder pageBuilder) {
        pageBuilder
                .page("rentDetails")
                .pageLabel("Rent details")
                .label("caseReference", "Case number:")
                .label("rentDetails-content", 
                        """
                        ---
                        <section tabindex="0">
                            <p class="govuk-body">
                                Please provide details about the current rental agreement.
                            </p>
                        </section>
                        """)
                .mandatory(PCSCase::getRentAmount)
                .mandatory(PCSCase::getRentPaymentFrequency)
                .mandatory(PCSCase::getOtherRentFrequency, "rentPaymentFrequency=\"OTHER\"")
                .mandatory(PCSCase::getDailyRentChargeAmount, "rentPaymentFrequency=\"OTHER\"");
    }
}
