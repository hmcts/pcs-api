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
import java.time.LocalDate;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class HouseholdCircumstances {

    @CCD
    private YesOrNo dependantChildren;

    @CCD
    private YesOrNo shareIncomeExpenseDetails;

    @CCD
    private YesOrNo incomeFromJobs;

    @CCD(typeOverride = FieldType.MoneyGBP)
    @JacksonMoneyGBP
    private BigDecimal incomeFromJobsAmount;

    @CCD
    private String incomeFromJobsFrequency;

    @CCD
    private YesOrNo pension;

    @CCD(typeOverride = FieldType.MoneyGBP)
    @JacksonMoneyGBP
    private BigDecimal pensionAmount;

    @CCD
    private String pensionFrequency;

    @CCD
    private YesOrNo universalCredit;

    @CCD
    private LocalDate ucApplicationDate;

    @CCD(typeOverride = FieldType.MoneyGBP)
    @JacksonMoneyGBP
    private BigDecimal universalCreditAmount;

    @CCD
    private String universalCreditFrequency;

    @CCD
    private YesOrNo otherBenefits;

    @CCD(typeOverride = FieldType.MoneyGBP)
    @JacksonMoneyGBP
    private BigDecimal otherBenefitsAmount;

    @CCD
    private String otherBenefitsFrequency;

    @CCD
    private YesOrNo moneyFromElsewhere;

    @CCD
    private String moneyFromElsewhereDetails;

}
