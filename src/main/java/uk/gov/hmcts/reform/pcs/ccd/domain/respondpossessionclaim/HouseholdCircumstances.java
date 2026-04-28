package uk.gov.hmcts.reform.pcs.ccd.domain.respondpossessionclaim;


import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.gov.hmcts.ccd.sdk.api.CCD;
import uk.gov.hmcts.ccd.sdk.type.FieldType;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.annotation.JacksonMoneyGBP;
import uk.gov.hmcts.reform.pcs.ccd.domain.YesNoNotSure;

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
    private YesOrNo shareAdditionalCircumstances;

    @CCD(max = 500)
    private String additionalCircumstancesDetails;

    @CCD
    private YesOrNo exceptionalHardship;

    @CCD(max = 500)
    private String exceptionalHardshipDetails;

    @CCD(max = 500)
    private String dependantChildrenDetails;

    @CCD
    private YesOrNo otherDependants;

    @CCD(max = 500)
    private String otherDependantDetails;

    @CCD
    private YesOrNo otherTenants;

    @CCD(max = 500)
    private String otherTenantsDetails;

    @CCD
    private YesNoNotSure alternativeAccommodation;

    @CCD
    private LocalDate alternativeAccommodationTransferDate;

    @CCD
    private YesOrNo shareIncomeExpenseDetails;
    private IncomeExpenseDetails householdBills;

    @CCD
    private IncomeExpenseDetails loanPayments;

    @CCD
    private IncomeExpenseDetails childSpousalMaintenance;

    @CCD
    private IncomeExpenseDetails mobilePhone;

    @CCD
    private IncomeExpenseDetails groceryShopping;

    @CCD
    private IncomeExpenseDetails fuelParkingTransport;

    @CCD
    private IncomeExpenseDetails schoolCosts;

    @CCD
    private IncomeExpenseDetails clothing;

    @CCD
    private IncomeExpenseDetails otherExpenses;

    @CCD
    private YesOrNo incomeFromJobs;

    @CCD(typeOverride = FieldType.MoneyGBP)
    @JacksonMoneyGBP
    private BigDecimal incomeFromJobsAmount;

    @CCD
    private RecurrenceFrequency incomeFromJobsFrequency;

    @CCD
    private YesOrNo pension;

    @CCD(typeOverride = FieldType.MoneyGBP)
    @JacksonMoneyGBP
    private BigDecimal pensionAmount;

    @CCD
    private RecurrenceFrequency pensionFrequency;

    @CCD
    private YesOrNo universalCredit;

    @CCD
    private LocalDate ucApplicationDate;

    @CCD(typeOverride = FieldType.MoneyGBP)
    @JacksonMoneyGBP
    private BigDecimal universalCreditAmount;

    @CCD
    private RecurrenceFrequency universalCreditFrequency;

    @CCD
    private YesOrNo otherBenefits;

    @CCD(typeOverride = FieldType.MoneyGBP)
    @JacksonMoneyGBP
    private BigDecimal otherBenefitsAmount;

    @CCD
    private RecurrenceFrequency otherBenefitsFrequency;

    @CCD
    private YesOrNo moneyFromElsewhere;

    @CCD
    @Size(max = 500)
    private String moneyFromElsewhereDetails;

}
