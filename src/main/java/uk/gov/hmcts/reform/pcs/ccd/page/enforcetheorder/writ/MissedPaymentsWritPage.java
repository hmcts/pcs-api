package uk.gov.hmcts.reform.pcs.ccd.page.enforcetheorder.writ;

import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.EnforcementOrder;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.writ.WritDetails;
import uk.gov.hmcts.reform.pcs.ccd.page.enforcetheorder.ShowConditionsEnforcementType;

import static uk.gov.hmcts.reform.pcs.ccd.page.CommonPageContent.SAVE_AND_RETURN;

public class MissedPaymentsWritPage implements CcdPageConfiguration {

    @Override
    public void addTo(PageBuilder pageBuilder) {
        pageBuilder
            .page("missedPaymentsWrit")
            .pageLabel("Missed payments")
            .label("missedPaymentsWrit-line-separator", "---")
            .showCondition(ShowConditionsEnforcementType.WRIT_FLOW)
            .complex(PCSCase::getEnforcementOrder)
            .complex(EnforcementOrder::getWritDetails)
            .mandatory(WritDetails::getDefendantMissedPayments)
            .done()
            .done()
            .label("missedPaymentsWrit-save-and-return", SAVE_AND_RETURN);
    }

}
