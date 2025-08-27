package uk.gov.hmcts.reform.pcs.ccd.page.createpossessionclaim;

import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;

/**
 * Placeholder page configuration for the Details of Rent Arrears section. Full
 * implementation will be done in another ticket
 */
public class DetailsOfRentArrears implements CcdPageConfiguration {

    @Override
    public void addTo(PageBuilder pageBuilder) {
        pageBuilder
                .page("detailsOfRentArrears")
                .pageLabel("Details of rent arrears (placeholder)")
                .label("detailsOfRentArrears-info",
                        """
                  ---
                    Under Development
                  """);
    }
}
