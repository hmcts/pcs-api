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
public class HouseholdCircumstances {

    @CCD
    private YesOrNo dependantChildren;

    @CCD
    private YesOrNo householdBills;

    @CCD(typeOverride = FieldType.MoneyGBP)
    @JacksonMoneyGBP
    private BigDecimal householdBillsAmount;

    @CCD
    private Frequency householdBillsFrequency;

    @CCD
    private YesOrNo loanPayments;

    @CCD(typeOverride = FieldType.MoneyGBP)
    @JacksonMoneyGBP
    private BigDecimal loanPaymentsAmount;

    @CCD
    private Frequency loanPaymentsFrequency;

    @CCD
    private YesOrNo childSpousalMaintenance;

    @CCD(typeOverride = FieldType.MoneyGBP)
    @JacksonMoneyGBP
    private BigDecimal childSpousalMaintenanceAmount;

    @CCD
    private Frequency childSpousalMaintenanceFrequency;

    @CCD
    private YesOrNo mobilePhone;

    @CCD(typeOverride = FieldType.MoneyGBP)
    @JacksonMoneyGBP
    private BigDecimal mobilePhoneAmount;

    @CCD
    private Frequency mobilePhoneFrequency;

    @CCD
    private YesOrNo groceryShopping;

    @CCD(typeOverride = FieldType.MoneyGBP)
    @JacksonMoneyGBP
    private BigDecimal groceryShoppingAmount;

    @CCD
    private Frequency groceryShoppingFrequency;

    @CCD
    private YesOrNo fuelParkingTransport;

    @CCD(typeOverride = FieldType.MoneyGBP)
    @JacksonMoneyGBP
    private BigDecimal fuelParkingTransportAmount;

    @CCD
    private Frequency fuelParkingTransportFrequency;

    @CCD
    private YesOrNo schoolCosts;

    @CCD(typeOverride = FieldType.MoneyGBP)
    @JacksonMoneyGBP
    private BigDecimal schoolCostsAmount;

    @CCD
    private Frequency schoolCostsFrequency;

    @CCD
    private YesOrNo clothing;

    @CCD(typeOverride = FieldType.MoneyGBP)
    @JacksonMoneyGBP
    private BigDecimal clothingAmount;

    @CCD
    private Frequency clothingFrequency;

    @CCD
    private YesOrNo otherExpenses;

    @CCD(typeOverride = FieldType.MoneyGBP)
    @JacksonMoneyGBP
    private BigDecimal otherExpensesAmount;

    @CCD
    private Frequency otherExpensesFrequency;

}
