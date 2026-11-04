package uk.gov.hmcts.reform.pcs.enforcement.ccd.page.warrant;

import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.enforcement.domain.EnforcementOrder;
import uk.gov.hmcts.reform.pcs.enforcement.domain.warrant.WarrantDetails;
import uk.gov.hmcts.reform.pcs.enforcement.ccd.page.ShowConditionsEnforcementType;

import static uk.gov.hmcts.reform.pcs.ccd.page.CommonPageContent.SAVE_AND_RETURN;

public class MissedPaymentsPage implements CcdPageConfiguration {

    @Override
    public void addTo(PageBuilder pageBuilder) {
        pageBuilder
            .page("missedPayments")
            .pageLabel("Missed payments")
            .label("missedPayments-line-separator", "---")
            .showCondition(ShowConditionsEnforcementType.WARRANT_FLOW)
            .complex(PCSCase::getEnforcementOrder)
            .complex(EnforcementOrder::getWarrantDetails)
            .mandatory(WarrantDetails::getDefendantMissedPayments)
            .done()
            .done()
            .label("missedPayments-save-and-return", SAVE_AND_RETURN);
    }

}
