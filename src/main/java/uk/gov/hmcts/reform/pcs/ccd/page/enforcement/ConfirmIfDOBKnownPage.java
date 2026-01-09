package uk.gov.hmcts.reform.pcs.ccd.page.enforcement;

import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcement.DefendantsDOB;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcement.EnforcementOrder;

import static uk.gov.hmcts.reform.pcs.ccd.page.CommonPageContent.SAVE_AND_RETURN;

/**
 * Page for confirming defendants date of birth.
 */
public class ConfirmIfDOBKnownPage implements CcdPageConfiguration {

    @Override
    public void addTo(PageBuilder pageBuilder) {
        pageBuilder
            .page("confirmDefendantsDOB")
            .pageLabel("Confirm if you know the defendants' dates of birth")
            .label("confirmDefendantsDOB-line-separator", "---")
            .complex(PCSCase::getEnforcementOrder)
            .complex(EnforcementOrder::getDefendantsDOB)
            .mandatory(DefendantsDOB::getDefendantsDOBKnown)
            .done()
            .done()
            .label("confirmDefendantsDOB-save-and-return", SAVE_AND_RETURN);
    }
}

