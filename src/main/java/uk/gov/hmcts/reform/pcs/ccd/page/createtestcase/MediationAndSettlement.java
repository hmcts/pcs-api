package uk.gov.hmcts.reform.pcs.ccd.page.createtestcase;

import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;

/**
 * Placeholder page configuration for the Mediation and Settlement section.
 * To be implemented in HDPI-1355.
 */
public class MediationAndSettlement implements CcdPageConfiguration {

    @Override
    public void addTo(PageBuilder pageBuilder) {
        pageBuilder
                .page("mediation and settlement")
                .pageLabel("Mediation and Settlement")
                .label("mediationAndSettlementInfo", 
                        """
                        ---
                        This feature is currently under development.
                        """);
    }
}
