package uk.gov.hmcts.reform.pcs.ccd.service.respondpossessionclaim;

import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.respondpossessionclaim.HouseholdCircumstances;
import uk.gov.hmcts.reform.pcs.ccd.entity.respondpossessionclaim.HouseholdCircumstancesEntity;

@Service
public class HouseholdCircumstancesService {

    public HouseholdCircumstancesEntity createHouseholdCircumstancesEntity(HouseholdCircumstances circumstances) {

        if (circumstances == null) {
            return null;
        }

       return HouseholdCircumstancesEntity.builder()
                .dependantChildren(circumstances.getDependantChildren())

                .householdBills(circumstances.getHouseholdBills())
                .householdBillsAmount(YesOrNo.YES.equals(circumstances.getHouseholdBills())
                                          ? circumstances.getHouseholdBillsAmount()
                                          : null)
                .householdBillsFrequency(YesOrNo.YES.equals(circumstances.getHouseholdBills())
                                             ? circumstances.getHouseholdBillsFrequency()
                                             : null)
                .loanPayments(circumstances.getLoanPayments())
                .loanPaymentsAmount(YesOrNo.YES.equals(circumstances.getLoanPayments())
                                        ? circumstances.getLoanPaymentsAmount()
                                        : null)
                .loanPaymentsFrequency(YesOrNo.YES.equals(circumstances.getLoanPayments())
                                           ? circumstances.getLoanPaymentsFrequency()
                                           : null)
                .childSpousalMaintenance(circumstances.getChildSpousalMaintenance())
                .childSpousalMaintenanceAmount(YesOrNo.YES.equals(circumstances.getChildSpousalMaintenance())
                                                   ? circumstances.getChildSpousalMaintenanceAmount()
                                                   : null)
                .childSpousalMaintenanceFrequency(YesOrNo.YES.equals(circumstances.getChildSpousalMaintenance())
                                                      ? circumstances.getChildSpousalMaintenanceFrequency()
                                                      : null)
                .mobilePhone(circumstances.getMobilePhone())
                .mobilePhoneAmount(YesOrNo.YES.equals(circumstances.getMobilePhone())
                                       ? circumstances.getMobilePhoneAmount()
                                       : null)
                .mobilePhoneFrequency(YesOrNo.YES.equals(circumstances.getMobilePhone())
                                          ? circumstances.getMobilePhoneFrequency()
                                          : null)
                .groceryShopping(circumstances.getGroceryShopping())
                .groceryShoppingAmount(YesOrNo.YES.equals(circumstances.getGroceryShopping())
                                           ? circumstances.getGroceryShoppingAmount()
                                           : null)
                .groceryShoppingFrequency(YesOrNo.YES.equals(circumstances.getGroceryShopping())
                                              ? circumstances.getGroceryShoppingFrequency()
                                              : null)
                .fuelTransport(circumstances.getFuelTransport())
                .fuelTransportAmount(YesOrNo.YES.equals(circumstances.getFuelTransport())
                                         ? circumstances.getFuelTransportAmount()
                                         : null)
                .fuelTransportFrequency(YesOrNo.YES.equals(circumstances.getFuelTransport())
                                            ? circumstances.getFuelTransportFrequency()
                                            : null)
                .schoolCosts(circumstances.getSchoolCosts())
                .schoolCostsAmount(YesOrNo.YES.equals(circumstances.getSchoolCosts())
                                       ? circumstances.getSchoolCostsAmount()
                                       : null)
                .schoolCostsFrequency(YesOrNo.YES.equals(circumstances.getSchoolCosts())
                                          ? circumstances.getSchoolCostsFrequency()
                                          : null)
                .clothing(circumstances.getClothing())
                .clothingAmount(YesOrNo.YES.equals(circumstances.getClothing())
                                    ? circumstances.getClothingAmount()
                                    : null)
                .clothingFrequency(YesOrNo.YES.equals(circumstances.getClothing())
                                       ? circumstances.getClothingFrequency()
                                       : null)
                .otherExpenses(circumstances.getOtherExpenses())
                .otherExpensesAmount(YesOrNo.YES.equals(circumstances.getOtherExpenses())
                                         ? circumstances.getOtherExpensesAmount()
                                         : null)
                .otherExpensesFrequency(YesOrNo.YES.equals(circumstances.getOtherExpenses())
                                            ? circumstances.getOtherExpensesFrequency()
                                            : null).build();

    }
}
