package uk.gov.hmcts.reform.pcs.ccd.domain.respondpossessionclaim;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.gov.hmcts.ccd.sdk.api.CCD;
import uk.gov.hmcts.ccd.sdk.type.FieldType;
import uk.gov.hmcts.reform.pcs.ccd.annotation.JacksonMoneyGBP;
import uk.gov.hmcts.reform.pcs.ccd.domain.YesNoNotSure;

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
    private YesNoNotSure tenancyStartDateCorrect;

    @CCD
    private YesNoNotSure oweRentArrears;

    @CCD(typeOverride = FieldType.MoneyGBP)
    @JacksonMoneyGBP
    private BigDecimal rentArrearsAmount;

    @CCD
    private YesNoNotSure noticeReceived;

    @CCD
    private LocalDate noticeReceivedDate;
}
