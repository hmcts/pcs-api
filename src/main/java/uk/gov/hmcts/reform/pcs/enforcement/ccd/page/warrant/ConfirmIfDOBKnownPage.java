package uk.gov.hmcts.reform.pcs.enforcement.ccd.page.warrant;

import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.enforcement.domain.EnforcementOrder;
import uk.gov.hmcts.reform.pcs.enforcement.domain.warrant.WarrantDetails;
import uk.gov.hmcts.reform.pcs.enforcement.ccd.page.ShowConditionsEnforcementType;

import static uk.gov.hmcts.reform.pcs.ccd.page.CommonPageContent.SAVE_AND_RETURN;

/**
 * Page for confirming defendants date of birth.
 */
public class ConfirmIfDOBKnownPage implements CcdPageConfiguration {

    @Override
    public void addTo(PageBuilder pageBuilder) {
        pageBuilder
            .page("confirmDefendantsDOB")
            .pageLabel("Confirm if you know the defendants’ dates of birth")
            .showCondition(ShowConditionsEnforcementType.WARRANT_FLOW)
            .label("confirmDefendantsDOB-line-separator", "---")
            .complex(PCSCase::getEnforcementOrder)
            .complex(EnforcementOrder::getWarrantDetails)
            .mandatory(WarrantDetails::getDefendantsDOBKnown)
            .done()
            .done()
            .label("confirmDefendantsDOB-save-and-return", SAVE_AND_RETURN);
    }
}
