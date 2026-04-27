package uk.gov.hmcts.reform.pcs.ccd.domain.respondpossessionclaim;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.gov.hmcts.ccd.sdk.api.CCD;
import uk.gov.hmcts.ccd.sdk.type.FieldType;
import uk.gov.hmcts.reform.pcs.ccd.accesscontrol.CitizenAccess;
import uk.gov.hmcts.reform.pcs.ccd.annotation.JacksonMoneyGBP;
import uk.gov.hmcts.reform.pcs.ccd.domain.ContactPreferenceType;
import uk.gov.hmcts.reform.pcs.ccd.domain.LanguageUsed;
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

    @CCD
    private YesNoNotSure tenancyTypeCorrect;

    @CCD
    private String tenancyType;

    @CCD
    private YesNoNotSure tenancyStartDateCorrect;

    @CCD
    private LocalDate tenancyStartDate;

    @CCD
    private YesNoNotSure tenancyStartDateConfirmation;

    @CCD
    private YesNoNotSure rentArrearsAmountConfirmation;

    @CCD(typeOverride = FieldType.MoneyGBP)
    @JacksonMoneyGBP
    private BigDecimal rentArrearsAmount;

    @CCD
    private YesNoNotSure possessionNoticeReceived;

    @CCD
    private LocalDate noticeReceivedDate;

    @CCD
    private VerticalYesNo contactByText;

    @CCD
    private VerticalYesNo contactByPhone;

    @CCD
    private ContactPreferenceType preferenceType;

    @CCD
    private YesNoPreferNotToSay freeLegalAdvice;

    @CCD
    private LocalDate dateOfBirth;

    @CCD
    private VerticalYesNo defendantNameConfirmation;

    @CCD
    private VerticalYesNo correspondenceAddressConfirmation;

    @CCD
    private YesNoNotSure landlordRegistered;

    @CCD
    private YesNoNotSure writtenTerms;

    @CCD
    private VerticalYesNo disputeClaim;

    @CCD
    private String disputeClaimDetails;

    @CCD
    private YesNoNotSure landlordLicensed;

    @CCD(access = {CitizenAccess.class})
    private ReasonableAdjustments reasonableAdjustments;

    @CCD(access = {CitizenAccess.class})
    private HouseholdCircumstances householdCircumstances;

    @CCD(access = {CitizenAccess.class})
    private PaymentAgreement paymentAgreement;

    private LanguageUsed languageUsed;

    private EqualityAndDiversityQuestionsChoice equalityAndDiversityQuestionsChoice;
}
