package uk.gov.hmcts.reform.pcs.ccd.page.enforcetheorder.writ;

import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.EnforcementOrder;
import uk.gov.hmcts.reform.pcs.ccd.page.enforcetheorder.ShowConditionsWarrantOrWrit;

import static uk.gov.hmcts.reform.pcs.ccd.page.CommonPageContent.SAVE_AND_RETURN;

public class RepaymentsPlaceholder implements CcdPageConfiguration {

    @Override
    public void addTo(PageBuilder pageBuilder) {
        pageBuilder
            .page("repaymentsWrit")
            .pageLabel("Repayments (placeholder)")
            .showCondition(ShowConditionsWarrantOrWrit.WRIT_FLOW)
            .complex(PCSCase::getEnforcementOrder)
            .complex(EnforcementOrder::getWritDetails)
            .label("repaymentsWrit-content", "---")
            .done()
            .done()
            .label("repaymentsWrit-saveAndReturn", SAVE_AND_RETURN);
    }
}
