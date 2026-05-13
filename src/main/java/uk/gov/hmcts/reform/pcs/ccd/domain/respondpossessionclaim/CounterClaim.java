package uk.gov.hmcts.reform.pcs.ccd.domain.respondpossessionclaim;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.gov.hmcts.ccd.sdk.api.CCD;
import uk.gov.hmcts.ccd.sdk.type.FieldType;
import uk.gov.hmcts.reform.pcs.ccd.accesscontrol.DefendantAccess;
import uk.gov.hmcts.reform.pcs.ccd.annotation.JacksonMoneyGBP;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;

import java.math.BigDecimal;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CounterClaim {

    @CCD(access = {DefendantAccess.class})
    private VerticalYesNo isClaimAmountKnown;

    @CCD(typeOverride = FieldType.MoneyGBP, access = {DefendantAccess.class})
    @JacksonMoneyGBP
    private BigDecimal claimAmount;

    @CCD(typeOverride = FieldType.MoneyGBP, access = {DefendantAccess.class})
    @JacksonMoneyGBP
    private BigDecimal estimatedMaxClaimAmount;

    @CCD(access = {DefendantAccess.class})
    private CounterClaimType claimType;

    @CCD(access = {DefendantAccess.class}, max = 6800)
    private String counterClaimFor;

    @CCD(access = {DefendantAccess.class}, max = 6800)
    private String counterClaimReasons;

    @CCD(access = {DefendantAccess.class}, max = 6800)
    private String otherOrderRequestDetails;

    @CCD(access = {DefendantAccess.class}, max = 6800)
    private String otherOrderRequestFacts;

    @CCD(access = {DefendantAccess.class})
    private VerticalYesNo needHelpWithFees;

    @CCD(access = {DefendantAccess.class})
    private VerticalYesNo appliedForHwf;

    @CCD(access = {DefendantAccess.class})
    private String hwfReferenceNumber;

}
