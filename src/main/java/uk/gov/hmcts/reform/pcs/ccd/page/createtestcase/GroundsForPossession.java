package uk.gov.hmcts.reform.pcs.ccd.page.createtestcase;

import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;

/**
 * Placeholder page configuration for the Grounds for Possession section. 
 * To be implemented later.
 */
public class GroundsForPossession implements CcdPageConfiguration {

    @Override
    public void addTo(PageBuilder pageBuilder) {
        pageBuilder
                .page("grounds for possession")
                .pageLabel("Grounds for possession")
                .label("groundsForPossessionInfo",
                        """
                        ---
                        This feature is currently under development.
                        """);
    }
}
