package uk.gov.hmcts.reform.pcs.ccd.page.createpossessionclaim;

import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;

/**
 * Placeholder page configuration for the Checking Notice section. 
 * Full implementation will be done in another ticket - responses not captured at the moment.
 */
public class CheckingNotice implements CcdPageConfiguration {

    @Override
    public void addTo(PageBuilder pageBuilder) {
        pageBuilder
                .page("checkingNotice")
                .pageLabel("Notice of your intention to begin possession proceedings")
                .label("checkingNotice-info",
                        """
                  ---
                    Under Development
                  """)
                .mandatory(PCSCase::getClaimantName);
    }
}
