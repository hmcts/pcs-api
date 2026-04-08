package uk.gov.hmcts.reform.pcs.ccd.service.respondpossessionclaim;

import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.YesNoNotSure;
import uk.gov.hmcts.reform.pcs.ccd.domain.respondpossessionclaim.HouseholdCircumstances;
import uk.gov.hmcts.reform.pcs.ccd.domain.respondpossessionclaim.IncomeExpenseDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.respondpossessionclaim.RecurrenceFrequency;
import uk.gov.hmcts.reform.pcs.ccd.entity.respondpossessionclaim.HouseholdCircumstancesEntity;

import java.math.BigDecimal;

@Service
public class HouseholdCircumstancesService {

    public HouseholdCircumstancesEntity createHouseholdCircumstancesEntity(HouseholdCircumstances hc) {

        if (hc == null) {
            return null;
        }

        YesOrNo otherTenants = hc.getOtherTenants();
        YesNoNotSure alternativeAccommodation = hc.getAlternativeAccommodation();

        return HouseholdCircumstancesEntity.builder()
            .dependantChildren(hc.getDependantChildren())
            .dependantChildren(hc.getDependantChildren())
            .dependantChildrenDetails(hc.getDependantChildrenDetails())
            .otherDependants(hc.getOtherDependants())
            .otherDependantDetails(hc.getOtherDependantDetails())
            .otherTenants(otherTenants)
            .otherTenantsDetails(otherTenants == YesOrNo.YES ? hc.getOtherTenantsDetails() : null)
            .alternativeAccommodation(alternativeAccommodation)
            .alternativeAccommodationTransferDate(alternativeAccommodation == YesNoNotSure.YES
                                                      ? hc.getAlternativeAccommodationTransferDate() : null)
            .householdBills(getApplies(hc.getHouseholdBills()))
            .householdBillsAmount(getAmountIfYes(hc.getHouseholdBills()))
            .householdBillsFrequency(getFrequencyIfYes(hc.getHouseholdBills()))
            .loanPayments(getApplies(hc.getLoanPayments()))
            .loanPaymentsAmount(getAmountIfYes(hc.getLoanPayments()))
            .loanPaymentsFrequency(getFrequencyIfYes(hc.getLoanPayments()))
            .childSpousalMaintenance(getApplies(hc.getChildSpousalMaintenance()))
            .childSpousalMaintenanceAmount(getAmountIfYes(hc.getChildSpousalMaintenance()))
            .childSpousalMaintenanceFrequency(getFrequencyIfYes(hc.getChildSpousalMaintenance()))
            .mobilePhone(getApplies(hc.getMobilePhone()))
            .mobilePhoneAmount(getAmountIfYes(hc.getMobilePhone()))
            .mobilePhoneFrequency(getFrequencyIfYes(hc.getMobilePhone()))
            .groceryShopping(getApplies(hc.getGroceryShopping()))
            .groceryShoppingAmount(getAmountIfYes(hc.getGroceryShopping()))
            .groceryShoppingFrequency(getFrequencyIfYes(hc.getGroceryShopping()))
            .fuelParkingTransport(getApplies(hc.getFuelParkingTransport()))
            .fuelParkingTransportAmount(getAmountIfYes(hc.getFuelParkingTransport()))
            .fuelParkingTransportFrequency(getFrequencyIfYes(hc.getFuelParkingTransport()))
            .schoolCosts(getApplies(hc.getSchoolCosts()))
            .schoolCostsAmount(getAmountIfYes(hc.getSchoolCosts()))
            .schoolCostsFrequency(getFrequencyIfYes(hc.getSchoolCosts()))
            .clothing(getApplies(hc.getClothing()))
            .clothingAmount(getAmountIfYes(hc.getClothing()))
            .clothingFrequency(getFrequencyIfYes(hc.getClothing()))
            .otherExpenses(getApplies(hc.getOtherExpenses()))
            .otherExpensesAmount(getAmountIfYes(hc.getOtherExpenses()))
            .otherExpensesFrequency(getFrequencyIfYes(hc.getOtherExpenses()))
            .build();
    }

    private YesOrNo getApplies(IncomeExpenseDetails details) {
        return details != null ? details.getApplies() : null;
    }

    private BigDecimal getAmountIfYes(IncomeExpenseDetails details) {
        return details != null && YesOrNo.YES.equals(details.getApplies()) ? details.getAmount() : null;
    }

    private RecurrenceFrequency getFrequencyIfYes(IncomeExpenseDetails details) {
        return details != null && YesOrNo.YES.equals(details.getApplies()) ? details.getFrequency() : null;
    }
}
