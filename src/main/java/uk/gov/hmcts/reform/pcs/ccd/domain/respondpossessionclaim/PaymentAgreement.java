package uk.gov.hmcts.reform.pcs.ccd.domain.respondpossessionclaim;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.gov.hmcts.ccd.sdk.api.CCD;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.YesNoNotSure;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaymentAgreement {

    @CCD
    private YesOrNo anyPaymentsMade;
    
    @CCD(max = 500)
    private String paymentDetails;

    @CCD(searchable = false)
    private YesNoNotSure repaymentPlanAgreed;

    @CCD
    private String repaymentAgreedDetails;

}
