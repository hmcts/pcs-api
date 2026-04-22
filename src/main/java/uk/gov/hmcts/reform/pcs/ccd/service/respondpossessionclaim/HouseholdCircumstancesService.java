package uk.gov.hmcts.reform.pcs.ccd.service.respondpossessionclaim;

import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.YesNoNotSure;
import uk.gov.hmcts.reform.pcs.ccd.domain.respondpossessionclaim.HouseholdCircumstances;
import uk.gov.hmcts.reform.pcs.ccd.domain.respondpossessionclaim.IncomeExpenseDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.respondpossessionclaim.RecurrenceFrequency;
import uk.gov.hmcts.reform.pcs.ccd.domain.respondpossessionclaim.RegularExpenseType;
import uk.gov.hmcts.reform.pcs.ccd.entity.respondpossessionclaim.HouseholdCircumstancesEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.respondpossessionclaim.RegularExpenseEntity;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class HouseholdCircumstancesService {

    public HouseholdCircumstancesEntity createHouseholdCircumstancesEntity(HouseholdCircumstances hc) {

        if (hc == null) {
            return null;
        }

        VerticalYesNo otherTenants = hc.getOtherTenants();
        YesNoNotSure alternativeAccommodation = hc.getAlternativeAccommodation();

        HouseholdCircumstancesEntity hcEntity = HouseholdCircumstancesEntity.builder()
            .dependantChildren(hc.getDependantChildren())
            .dependantChildrenDetails(hc.getDependantChildrenDetails())
            .shareAdditionalCircumstances(hc.getShareAdditionalCircumstances())
            .additionalCircumstancesDetails(hc.getAdditionalCircumstancesDetails())
            .exceptionalHardship(hc.getExceptionalHardship())
            .exceptionalHardshipDetails(hc.getExceptionalHardshipDetails())
            .otherDependants(hc.getOtherDependants())
            .otherDependantDetails(hc.getOtherDependantDetails())
            .otherTenants(otherTenants)
            .otherTenantsDetails(otherTenants == VerticalYesNo.YES ? hc.getOtherTenantsDetails() : null)
            .alternativeAccommodation(alternativeAccommodation)
            .alternativeAccommodationTransferDate(alternativeAccommodation == YesNoNotSure.YES
                                                      ? hc.getAlternativeAccommodationTransferDate() : null)
            .build();

        List<RegularExpenseEntity> expenses = createRegularExpenseEntities(hc);
        expenses.forEach(hcEntity::addRegularExpense);

        return hcEntity;
    }


    private List<RegularExpenseEntity> createRegularExpenseEntities(HouseholdCircumstances hc) {

        Map<RegularExpenseType, IncomeExpenseDetails> expenseMap = new HashMap<>();

        addIfNotNull(expenseMap, RegularExpenseType.HOUSEHOLD_BILLS, hc.getHouseholdBills());
        addIfNotNull(expenseMap, RegularExpenseType.LOAN_PAYMENTS, hc.getLoanPayments());
        addIfNotNull(expenseMap, RegularExpenseType.CHILD_SPOUSAL_MAINTENANCE, hc.getChildSpousalMaintenance());
        addIfNotNull(expenseMap, RegularExpenseType.MOBILE_PHONE, hc.getMobilePhone());
        addIfNotNull(expenseMap, RegularExpenseType.GROCERY_SHOPPING, hc.getGroceryShopping());
        addIfNotNull(expenseMap, RegularExpenseType.FUEL_PARKING_TRANSPORT, hc.getFuelParkingTransport());
        addIfNotNull(expenseMap, RegularExpenseType.SCHOOL_COSTS, hc.getSchoolCosts());
        addIfNotNull(expenseMap, RegularExpenseType.CLOTHING, hc.getClothing());
        addIfNotNull(expenseMap, RegularExpenseType.OTHER, hc.getOtherExpenses());

        return expenseMap.entrySet().stream()
            .filter(entry -> YesOrNo.YES.equals(getApplies(entry.getValue())))
            .map(entry -> RegularExpenseEntity.builder()
                .expenseType(entry.getKey())
                .amount(getAmountIfYes(entry.getValue()))
                .expenseFrequency(getFrequencyIfYes(entry.getValue()))
                .build()
            )
            .toList();
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

    private void addIfNotNull(Map<RegularExpenseType, IncomeExpenseDetails> map, RegularExpenseType type,
                              IncomeExpenseDetails details) {
        if (details != null) {
            map.put(type, details);
        }
    }
}
