package uk.gov.hmcts.reform.pcs.ccd.domain.citizenclaim;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.gov.hmcts.ccd.sdk.api.CCD;
import uk.gov.hmcts.reform.pcs.ccd.annotation.JacksonMoneyGBP;

import java.math.BigDecimal;
import java.time.LocalDate;

import static uk.gov.hmcts.ccd.sdk.type.FieldType.MoneyGBP;

@Builder(toBuilder = true)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CitizenClaimRentPeriodEntry {

    private LocalDate periodStart;

    private LocalDate dateDue;

    @CCD(typeOverride = MoneyGBP)
    @JacksonMoneyGBP
    private BigDecimal amountDue;

    private LocalDate datePaid;

    @CCD(typeOverride = MoneyGBP)
    @JacksonMoneyGBP
    private BigDecimal amountPaid;

}
