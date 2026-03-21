package uk.gov.hmcts.reform.pcs.ccd.domain.respondpossessionclaim;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.gov.hmcts.ccd.sdk.api.CCD;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;

import java.math.BigDecimal;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class HouseholdCircumstances {

    @CCD
    private YesOrNo dependantChildren;

    // Income & Expense Control Field (HDPI-3764)
    @CCD
    private YesOrNo shareIncomeExpenseDetails;

    // Regular Income Fields (HDPI-3764)
    @CCD
    private YesOrNo incomeFromJobs;

    @CCD
    private BigDecimal incomeFromJobsAmount;

    @CCD
    private String incomeFromJobsFrequency;

    @CCD
    private YesOrNo pension;

    @CCD
    private BigDecimal pensionAmount;

    @CCD
    private String pensionFrequency;

    @CCD
    private YesOrNo universalCreditIncome;

    @CCD
    private YesOrNo otherBenefits;

    @CCD
    private BigDecimal otherBenefitsAmount;

    @CCD
    private String otherBenefitsFrequency;

    @CCD
    private YesOrNo moneyFromElsewhere;

    @CCD
    private String moneyFromElsewhereDetails;

}
