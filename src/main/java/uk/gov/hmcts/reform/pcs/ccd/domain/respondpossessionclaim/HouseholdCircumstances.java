package uk.gov.hmcts.reform.pcs.ccd.domain.respondpossessionclaim;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.gov.hmcts.ccd.sdk.api.CCD;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class HouseholdCircumstances {

    @CCD
    private YesOrNo dependantChildren;

    @CCD
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

}
