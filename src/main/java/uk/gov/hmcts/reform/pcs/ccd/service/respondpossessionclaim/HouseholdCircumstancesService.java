package uk.gov.hmcts.reform.pcs.ccd.service.respondpossessionclaim;

import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.respondpossessionclaim.HouseholdCircumstances;
import uk.gov.hmcts.reform.pcs.ccd.entity.respondpossessionclaim.HouseholdCircumstancesEntity;

@Service
public class HouseholdCircumstancesService {

    public HouseholdCircumstancesEntity createHouseholdCircumstancesEntity(HouseholdCircumstances hc) {

        if (hc == null) {
            return null;
        }
        return HouseholdCircumstancesEntity.builder()
            .dependantChildren(hc.getDependantChildren())
            .householdBills(hc.getHouseholdBills())
            .householdBillsAmount(mapIfYes(hc.getHouseholdBills(), hc.getHouseholdBillsAmount()))
            .householdBillsFrequency(mapIfYes(hc.getHouseholdBills(), hc.getHouseholdBillsFrequency()))
            .loanPayments(hc.getLoanPayments())
            .loanPaymentsAmount(mapIfYes(hc.getLoanPayments(), hc.getLoanPaymentsAmount()))
            .loanPaymentsFrequency(mapIfYes(hc.getLoanPayments(), hc.getLoanPaymentsFrequency()))
            .childSpousalMaintenance(hc.getChildSpousalMaintenance())
            .childSpousalMaintenanceAmount(mapIfYes(hc.getChildSpousalMaintenance(),
                                                    hc.getChildSpousalMaintenanceAmount()))
            .childSpousalMaintenanceFrequency(mapIfYes(hc.getChildSpousalMaintenance(),
                                                       hc.getChildSpousalMaintenanceFrequency()))
            .mobilePhone(hc.getMobilePhone())
            .mobilePhoneAmount(mapIfYes(hc.getMobilePhone(), hc.getMobilePhoneAmount()))
            .mobilePhoneFrequency(mapIfYes(hc.getMobilePhone(), hc.getMobilePhoneFrequency()))
            .groceryShopping(hc.getGroceryShopping())
            .groceryShoppingAmount(mapIfYes(hc.getGroceryShopping(), hc.getGroceryShoppingAmount()))
            .groceryShoppingFrequency(mapIfYes(hc.getGroceryShopping(), hc.getGroceryShoppingFrequency()))
            .fuelParkingTransport(hc.getFuelParkingTransport())
            .fuelParkingTransportAmount(mapIfYes(hc.getFuelParkingTransport(), hc.getFuelParkingTransportAmount()))
            .fuelParkingTransportFrequency(mapIfYes(hc.getFuelParkingTransport(),hc.getFuelParkingTransportFrequency()))
            .schoolCosts(hc.getSchoolCosts())
            .schoolCostsAmount(mapIfYes(hc.getSchoolCosts(), hc.getSchoolCostsAmount()))
            .schoolCostsFrequency(mapIfYes(hc.getSchoolCosts(), hc.getSchoolCostsFrequency()))
            .clothing(hc.getClothing())
            .clothingAmount(mapIfYes(hc.getClothing(), hc.getClothingAmount()))
            .clothingFrequency(mapIfYes(hc.getClothing(), hc.getClothingFrequency()))
            .otherExpenses(hc.getOtherExpenses())
            .otherExpensesAmount(mapIfYes(hc.getOtherExpenses(), hc.getOtherExpensesAmount()))
            .otherExpensesFrequency(mapIfYes(hc.getOtherExpenses(), hc.getOtherExpensesFrequency()))
            .build();
    }

    private <T> T mapIfYes(YesOrNo condition, T value) {
        return YesOrNo.YES.equals(condition) ? value : null;
    }
}
