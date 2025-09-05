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
                .label("rentDetails-content", 
                        """
                        """)
                .mandatory(PCSCase::getCurrentRent, "rentFrequency!=\"OTHER\"")
                .mandatory(PCSCase::getRentFrequency)
                .mandatory(PCSCase::getOtherRentFrequency, "rentFrequency=\"OTHER\"")
                .mandatory(PCSCase::getDailyRentChargeAmount, "rentFrequency=\"OTHER\"");
    }
}
