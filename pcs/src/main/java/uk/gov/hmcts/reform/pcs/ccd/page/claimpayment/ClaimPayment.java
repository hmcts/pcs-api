package uk.gov.hmcts.reform.pcs.ccd.page.claimpayment;

import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;

public class ClaimPayment implements CcdPageConfiguration {

    @Override
    public void addTo(PageBuilder pageBuilder) {
        pageBuilder
            .page("Make payment")
            .mandatory(PCSCase::getPaymentType);
    }
}
