package uk.gov.hmcts.reform.pcs.ccd3.page.claimpayment;

import uk.gov.hmcts.reform.pcs.ccd3.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd3.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd3.domain.PCSCase;

public class ClaimPayment implements CcdPageConfiguration {

    @Override
    public void addTo(PageBuilder pageBuilder) {
        pageBuilder
            .page("Make payment")
            .mandatory(PCSCase::getPaymentType);
    }
}
