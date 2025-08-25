package uk.gov.hmcts.reform.pcs.ccd.page.createpossessionclaim;

import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;

/**
 * Placeholder page configuration for the Rent Details section. Full
 * implementation will be done in another ticket - responses not captured at the
 * moment.
 */
public class RentDetails implements CcdPageConfiguration {

    @Override
    public void addTo(PageBuilder pageBuilder) {
        pageBuilder
                .page("rentDetails")
                .pageLabel("Rent details (placeholder)")
                .label("rentDetails-info",
                        """
                  ---
                    Under Development
                  """);
    }
}
