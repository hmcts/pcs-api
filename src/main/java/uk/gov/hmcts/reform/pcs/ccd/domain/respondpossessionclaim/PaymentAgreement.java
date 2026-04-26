package uk.gov.hmcts.reform.pcs.ccd.domain.respondpossessionclaim;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.gov.hmcts.ccd.sdk.api.CCD;
import uk.gov.hmcts.ccd.sdk.type.FieldType;
import uk.gov.hmcts.reform.pcs.ccd.annotation.JacksonMoneyGBP;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.YesNoNotSure;

import java.math.BigDecimal;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaymentAgreement {

    @CCD
    private VerticalYesNo anyPaymentsMade;

    @CCD(max = 500)
    private String paymentDetails;

    @CCD(searchable = false)
    private YesNoNotSure repaymentPlanAgreed;

    @CCD
    private String repaymentAgreedDetails;

    @CCD
    private VerticalYesNo repayArrearsInstalments;

    @CCD(typeOverride = FieldType.MoneyGBP)
    @JacksonMoneyGBP
    private BigDecimal additionalRentContribution;

    @CCD
    private String additionalContributionFrequency;

}
