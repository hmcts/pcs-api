package uk.gov.hmcts.reform.pcs.ccd.domain.respondpossessionclaim;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.gov.hmcts.ccd.sdk.api.CCD;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.YesNoNotSure;

import java.time.LocalDate;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class HouseholdCircumstances {

    @CCD
    private VerticalYesNo dependantChildren;

    @CCD
    private VerticalYesNo shareAdditionalCircumstances;

    @CCD(max = 500)
    private String additionalCircumstancesDetails;

    @CCD
    private VerticalYesNo exceptionalHardship;

    @CCD(max = 500)
    private String exceptionalHardshipDetails;

    @CCD(max = 500)
    private String dependantChildrenDetails;

    @CCD
    private VerticalYesNo otherDependants;

    @CCD(max = 500)
    private String otherDependantDetails;

    @CCD
    private VerticalYesNo otherTenants;

    @CCD(max = 500)
    private String otherTenantsDetails;

    @CCD
    private YesNoNotSure alternativeAccommodation;

    @CCD
    private LocalDate alternativeAccommodationTransferDate;

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
