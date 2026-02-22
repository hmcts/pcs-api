package uk.gov.hmcts.reform.pcs.ccd.page.enforcetheorder.writ;

import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.EnforcementOrder;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.writ.WritDetails;
import uk.gov.hmcts.reform.pcs.ccd.page.CommonPageContent;
import uk.gov.hmcts.reform.pcs.ccd.page.enforcetheorder.ShowConditionsEnforcementType;

public class ToggleClaimSentToHighCourtPlaceholder implements CcdPageConfiguration {

    @Override
    public void addTo(PageBuilder pageBuilder) {
        pageBuilder
            .page("toggleClaimSentToHighCourtPlaceholder")
            .pageLabel("Placeholder to simulate if claim has been sent to High Court")
            .showCondition(ShowConditionsEnforcementType.WRIT_FLOW)
            .label("toggleClaimSentToHighCourtPlaceholder-line-separator", "---")
            .complex(PCSCase::getEnforcementOrder)
            .complex(EnforcementOrder::getWritDetails)
            .mandatory(WritDetails::getHasClaimTransferredToHighCourt)
            .done()
            .done()
            .label("toggleClaimSentToHighCourtPlaceholder-saveAndReturn", CommonPageContent.SAVE_AND_RETURN);
    }
}
