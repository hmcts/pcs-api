package uk.gov.hmcts.reform.pcs.ccd3.page.createpossessionclaim;

import uk.gov.hmcts.reform.pcs.ccd3.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd3.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd3.domain.PCSCase;

/**
 * Placeholder page configuration for the Grounds for Possession section.
 * Full implementation will be done in another ticket - responses not captured at the moment.
 */
public class GroundsForPossession implements CcdPageConfiguration {

    @Override
    public void addTo(PageBuilder pageBuilder) {
        pageBuilder
                .page("groundsForPossession")
                .pageLabel("Grounds for possession (placeholder)")
                .label("groundsForPossession-lineSeparator", "---")
                .mandatory(PCSCase::getGroundsForPossession);

    }
}
