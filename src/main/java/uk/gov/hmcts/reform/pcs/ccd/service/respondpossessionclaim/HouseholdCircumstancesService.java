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

            .householdBills(hc.getHouseholdBills().getApplies())
            .householdBillsAmount(mapIfYes(hc.getHouseholdBills().getApplies(), hc.getHouseholdBills().getAmount()))
            .householdBillsFrequency(mapIfYes(hc.getHouseholdBills().getApplies(),
                                              hc.getHouseholdBills().getFrequency()))
            .loanPayments(hc.getLoanPayments().getApplies())
            .loanPaymentsAmount(mapIfYes(hc.getLoanPayments().getApplies(), hc.getLoanPayments().getAmount()))
            .loanPaymentsFrequency(mapIfYes(hc.getLoanPayments().getApplies(), hc.getLoanPayments().getFrequency()))
            .childSpousalMaintenance(hc.getChildSpousalMaintenance().getApplies())
            .childSpousalMaintenanceAmount(mapIfYes(hc.getChildSpousalMaintenance().getApplies(),
                                                    hc.getChildSpousalMaintenance().getAmount()))
            .childSpousalMaintenanceFrequency(mapIfYes(hc.getChildSpousalMaintenance().getApplies(),
                                                       hc.getChildSpousalMaintenance().getFrequency()))
            .mobilePhone(hc.getMobilePhone().getApplies())
            .mobilePhoneAmount(mapIfYes(hc.getMobilePhone().getApplies(),
                                        hc.getMobilePhone().getAmount()))
            .mobilePhoneFrequency(mapIfYes(hc.getMobilePhone().getApplies(),
                                           hc.getMobilePhone().getFrequency()))
            .groceryShopping(hc.getGroceryShopping().getApplies())
            .groceryShoppingAmount(mapIfYes(hc.getGroceryShopping().getApplies(),
                                            hc.getGroceryShopping().getAmount()))
            .groceryShoppingFrequency(mapIfYes(hc.getGroceryShopping().getApplies(),
                                               hc.getGroceryShopping().getFrequency()))
            .fuelParkingTransport(hc.getFuelParkingTransport().getApplies())
            .fuelParkingTransportAmount(mapIfYes(hc.getFuelParkingTransport().getApplies(),
                                                 hc.getFuelParkingTransport().getAmount()))
            .fuelParkingTransportFrequency(mapIfYes(hc.getFuelParkingTransport().getApplies(),
                                                    hc.getFuelParkingTransport().getFrequency()))
            .schoolCosts(hc.getSchoolCosts().getApplies())
            .schoolCostsAmount(mapIfYes(hc.getSchoolCosts().getApplies(),
                                        hc.getSchoolCosts().getAmount()))
            .schoolCostsFrequency(mapIfYes(hc.getSchoolCosts().getApplies(),
                                           hc.getSchoolCosts().getFrequency()))
            .clothing(hc.getClothing().getApplies())
            .clothingAmount(mapIfYes(hc.getClothing().getApplies(),
                                     hc.getClothing().getAmount()))
            .clothingFrequency(mapIfYes(hc.getClothing().getApplies(),
                                        hc.getClothing().getFrequency()))
            .otherExpenses(hc.getOtherExpenses().getApplies())
            .otherExpensesAmount(mapIfYes(hc.getOtherExpenses().getApplies(),
                                          hc.getOtherExpenses().getAmount()))
            .otherExpensesFrequency(mapIfYes(hc.getOtherExpenses().getApplies(),
                                             hc.getOtherExpenses().getFrequency()))

            .build();
    }
    private <T> T mapIfYes(YesOrNo condition, T value) {
        return YesOrNo.YES.equals(condition) ? value : null;
    }
}
