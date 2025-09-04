package uk.gov.hmcts.reform.pcs.ccd.page.createpossessionclaim;

import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;

/**
 * Page configuration for the Grounds for Possession section.
 */
public class GroundsForPossession implements CcdPageConfiguration {

    @Override
    public void addTo(PageBuilder pageBuilder) {
        pageBuilder
                .page("groundsForPossession")
                .pageLabel("Grounds for possession")
                .label("groundsForPossession-lineSeparator", "---")
                .mandatory(PCSCase::getGroundsForPossession);
    }
}
