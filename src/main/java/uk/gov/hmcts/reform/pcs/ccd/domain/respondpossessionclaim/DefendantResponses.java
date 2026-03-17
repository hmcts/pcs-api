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
import uk.gov.hmcts.reform.pcs.ccd.domain.YesNoPreferNotToSay;

import java.math.BigDecimal;
import java.time.LocalDate;

import static uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase.MAX_MONETARY_AMOUNT;
import static uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase.MIN_MONETARY_AMOUNT;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DefendantResponses {

    @CCD
    private YesNoNotSure tenancyTypeCorrect;

    @CCD
    private LocalDate tenancyStartDate;

    @CCD
    private YesNoNotSure tenancyStartDateConfirmation;

    @CCD
    private YesNoNotSure rentArrearsAmountConfirmation;

    @CCD(
        typeOverride = FieldType.MoneyGBP,
        min = MIN_MONETARY_AMOUNT,
        max = MAX_MONETARY_AMOUNT
    )
    @JacksonMoneyGBP
    private BigDecimal rentArrearsAmount;

    @CCD
    private VerticalYesNo defendantNameConfirmation;

    @CCD
    private YesNoNotSure noticeReceived;

    @CCD
    private LocalDate noticeReceivedDate;

    @CCD
    private VerticalYesNo contactByEmail;

    @CCD
    private VerticalYesNo contactByText;

    @CCD
    private VerticalYesNo contactByPost;

    @CCD
    private VerticalYesNo contactByPhone;

    @CCD
    private YesNoPreferNotToSay receivedFreeLegalAdvice;

    @CCD
    private LocalDate dateOfBirth;

    @CCD
    private YesNoNotSure landlordRegistered;
}
