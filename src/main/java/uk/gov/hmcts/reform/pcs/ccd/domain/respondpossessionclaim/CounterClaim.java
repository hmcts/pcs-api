package uk.gov.hmcts.reform.pcs.ccd.domain.respondpossessionclaim;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.gov.hmcts.ccd.sdk.api.CCD;
import uk.gov.hmcts.ccd.sdk.type.FieldType;
import uk.gov.hmcts.reform.pcs.ccd.accesscontrol.CitizenAccess;
import uk.gov.hmcts.reform.pcs.ccd.annotation.JacksonMoneyGBP;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;

import java.math.BigDecimal;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CounterClaim {

    @CCD(access = {CitizenAccess.class})
    private VerticalYesNo isClaimAmountKnown;

    @CCD(typeOverride = FieldType.MoneyGBP, access = {CitizenAccess.class})
    @JacksonMoneyGBP
    private BigDecimal claimAmount;

    @CCD(typeOverride = FieldType.MoneyGBP, access = {CitizenAccess.class})
    @JacksonMoneyGBP
    private BigDecimal estimatedMaxClaimAmount;

    @CCD(access = {CitizenAccess.class})
    private CounterClaimType claimType;

    @CCD(access = {CitizenAccess.class}, max = 6800)
    private String counterclaimFor;

    @CCD(access = {CitizenAccess.class}, max = 6800)
    private String counterclaimReasons;

    @CCD(access = {CitizenAccess.class}, max = 6800)
    private String otherOrderRequestDetails;

    @CCD(access = {CitizenAccess.class}, max = 6800)
    private String otherOrderRequestFacts;

    @CCD(access = {CitizenAccess.class})
    private VerticalYesNo needHelpWithFees;

    @CCD(access = {CitizenAccess.class})
    private VerticalYesNo appliedForHwf;

    @CCD(access = {CitizenAccess.class})
    private String hwfReferenceNumber;

}
