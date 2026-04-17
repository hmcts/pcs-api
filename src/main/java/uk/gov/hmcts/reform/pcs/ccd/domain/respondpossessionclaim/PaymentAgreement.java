package uk.gov.hmcts.reform.pcs.ccd.domain.respondpossessionclaim;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.gov.hmcts.ccd.sdk.api.CCD;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.accesscontrol.CitizenAccess;
import uk.gov.hmcts.reform.pcs.ccd.domain.YesNoNotSure;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaymentAgreement {

    @CCD(access = {CitizenAccess.class})
    private YesOrNo anyPaymentsMade;

    @CCD(searchable = false,access = {CitizenAccess.class})
    private YesNoNotSure repaymentPlanAgreed;

    @CCD(access = {CitizenAccess.class})
    private String repaymentAgreedDetails;

}
