package uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.common;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.gov.hmcts.ccd.sdk.api.CCD;
import uk.gov.hmcts.ccd.sdk.type.FieldType;
import uk.gov.hmcts.reform.pcs.ccd.annotation.JacksonMoneyGBP;
import uk.gov.hmcts.reform.pcs.ccd.domain.RepaymentPreference;

import java.math.BigDecimal;

import static uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase.MAX_MONETARY_AMOUNT;
import static uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase.MIN_MONETARY_AMOUNT;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonNaming(PropertyNamingStrategies.UpperCamelCaseStrategy.class)
@NoArgsConstructor
@AllArgsConstructor
public class RepaymentCosts {

    @CCD(
        label = "How much do you want the defendants to repay?"
    )
    private RepaymentPreference repaymentChoice;

    @CCD(
        label = "Enter the amount that you want the defendants to repay",
        typeOverride = FieldType.MoneyGBP,
        min = MIN_MONETARY_AMOUNT,
        max = MAX_MONETARY_AMOUNT
    )
    @JacksonMoneyGBP
    private BigDecimal amountOfRepaymentCosts;

    private String repaymentSummaryMarkdown;

    private String statementOfTruthRepaymentSummaryMarkdown;

}
