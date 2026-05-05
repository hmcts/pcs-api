package uk.gov.hmcts.reform.pcs.ccd.domain.respondpossessionclaim;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.gov.hmcts.ccd.sdk.api.CCD;
import uk.gov.hmcts.ccd.sdk.type.FieldType;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.annotation.JacksonMoneyGBP;

import java.math.BigDecimal;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class IncomeExpenseDetails {

    @CCD
    private YesOrNo applies;

    @CCD(typeOverride = FieldType.MoneyGBP)
    @JacksonMoneyGBP
    private BigDecimal amount;

    @CCD
    private RecurrenceFrequency frequency;
}
