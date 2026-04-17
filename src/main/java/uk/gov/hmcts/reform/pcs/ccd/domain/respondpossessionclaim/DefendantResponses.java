package uk.gov.hmcts.reform.pcs.ccd.domain.respondpossessionclaim;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.gov.hmcts.ccd.sdk.api.CCD;
import uk.gov.hmcts.ccd.sdk.type.FieldType;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.accesscontrol.CitizenAccess;
import uk.gov.hmcts.reform.pcs.ccd.annotation.JacksonMoneyGBP;
import uk.gov.hmcts.reform.pcs.ccd.domain.ContactPreferenceType;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.YesNoNotSure;
import uk.gov.hmcts.reform.pcs.ccd.domain.YesNoPreferNotToSay;

import java.math.BigDecimal;
import java.time.LocalDate;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DefendantResponses {

    @CCD(access = {CitizenAccess.class})
    private YesNoNotSure tenancyTypeCorrect;

    @CCD(access = {CitizenAccess.class})
    private String tenancyType;

    @CCD(access = {CitizenAccess.class})
    private YesNoNotSure tenancyStartDateCorrect;

    @CCD(access = {CitizenAccess.class})
    private LocalDate tenancyStartDate;

    @CCD(access = {CitizenAccess.class})
    private YesNoNotSure tenancyStartDateConfirmation;

    @CCD(access = {CitizenAccess.class})
    private YesNoNotSure rentArrearsAmountConfirmation;

    @CCD(typeOverride = FieldType.MoneyGBP, access = {CitizenAccess.class})
    @JacksonMoneyGBP
    private BigDecimal rentArrearsAmount;

    @CCD(access = {CitizenAccess.class})
    private YesNoNotSure noticeReceived;

    @CCD(access = {CitizenAccess.class})
    private LocalDate noticeReceivedDate;

    @CCD(access = {CitizenAccess.class})
    private VerticalYesNo contactByText;

    @CCD(access = {CitizenAccess.class})
    private VerticalYesNo contactByPhone;

    @CCD(access = {CitizenAccess.class})
    private ContactPreferenceType preferenceType;

    @CCD(access = {CitizenAccess.class})
    private YesNoPreferNotToSay freeLegalAdvice;

    @CCD(access = {CitizenAccess.class})
    private LocalDate dateOfBirth;

    @CCD(access = {CitizenAccess.class})
    private VerticalYesNo defendantNameConfirmation;

    @CCD(access = {CitizenAccess.class})
    private YesNoNotSure landlordRegistered;

    @CCD(access = {CitizenAccess.class})
    private YesNoNotSure writtenTerms;

    @CCD(access = {CitizenAccess.class})
    private YesOrNo disputeClaim;

    @CCD(access = {CitizenAccess.class})
    private String disputeClaimDetails;

    @CCD(access = {CitizenAccess.class})
    private YesNoNotSure landlordLicensed;

    @CCD(access = {CitizenAccess.class})
    private ReasonableAdjustments reasonableAdjustments;

    @CCD(access = {CitizenAccess.class})
    private HouseholdCircumstances householdCircumstances;

    @CCD(access = {CitizenAccess.class})
    private PaymentAgreement paymentAgreement;

}
