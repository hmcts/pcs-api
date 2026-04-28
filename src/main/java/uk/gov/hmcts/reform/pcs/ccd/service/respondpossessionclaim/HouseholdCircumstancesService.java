package uk.gov.hmcts.reform.pcs.ccd.service.respondpossessionclaim;

import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.IncomeType;
import uk.gov.hmcts.reform.pcs.ccd.domain.YesNoNotSure;
import uk.gov.hmcts.reform.pcs.ccd.domain.respondpossessionclaim.HouseholdCircumstances;
import uk.gov.hmcts.reform.pcs.ccd.domain.respondpossessionclaim.IncomeExpenseDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.respondpossessionclaim.RecurrenceFrequency;
import uk.gov.hmcts.reform.pcs.ccd.domain.respondpossessionclaim.RegularExpenseType;
import uk.gov.hmcts.reform.pcs.ccd.entity.respondpossessionclaim.HouseholdCircumstancesEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.respondpossessionclaim.RegularExpenseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.respondpossessionclaim.RegularIncomeEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.respondpossessionclaim.RegularIncomeItemEntity;

import static uk.gov.hmcts.reform.pcs.ccd.util.YesOrNoConverter.toVerticalYesNo;
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
        YesOrNo otherTenants = hc.getOtherTenants();
        YesNoNotSure alternativeAccommodation = hc.getAlternativeAccommodation();

        HouseholdCircumstancesEntity hcEntity = HouseholdCircumstancesEntity.builder()
            .dependantChildren(toVerticalYesNo(hc.getDependantChildren()))
            .dependantChildrenDetails(hc.getDependantChildrenDetails())
            .shareAdditionalCircumstances(toVerticalYesNo(hc.getShareAdditionalCircumstances()))
            .additionalCircumstancesDetails(hc.getAdditionalCircumstancesDetails())
            .exceptionalHardship(toVerticalYesNo(hc.getExceptionalHardship()))
            .exceptionalHardshipDetails(hc.getExceptionalHardshipDetails())
            .otherDependants(toVerticalYesNo(hc.getOtherDependants()))
            .otherDependantDetails(hc.getOtherDependantDetails())
            .otherTenants(toVerticalYesNo(otherTenants))
            .otherTenantsDetails(otherTenants == YesOrNo.YES ? hc.getOtherTenantsDetails() : null)
            .alternativeAccommodation(alternativeAccommodation)
            .alternativeAccommodationTransferDate(alternativeAccommodation == YesNoNotSure.YES
                                                      ? hc.getAlternativeAccommodationTransferDate() : null)

            .shareIncomeExpenseDetails(toVerticalYesNo(hc.getShareIncomeExpenseDetails()))
            .universalCredit(toVerticalYesNo(hc.getUniversalCredit()))
            .ucApplicationDate(hc.getUcApplicationDate())
            .build();

        RegularIncomeEntity regularIncome = buildRegularIncome(hc);
        if (regularIncome != null) {
            hcEntity.setRegularIncomeEntity(regularIncome);
        }

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

    private RegularIncomeEntity buildRegularIncome(HouseholdCircumstances circumstances) {
        RegularIncomeEntity regularIncome = RegularIncomeEntity.builder()
            .otherIncomeDetails(circumstances.getMoneyFromElsewhere() == YesOrNo.YES
                                    ? circumstances.getMoneyFromElsewhereDetails() : null)
            .build();

        if (circumstances.getIncomeFromJobs() == YesOrNo.YES) {
            regularIncome.addItem(RegularIncomeItemEntity.builder()
                                      .incomeType(IncomeType.INCOME_FROM_JOBS)
                                      .amount(circumstances.getIncomeFromJobsAmount())
                                      .frequency(circumstances.getIncomeFromJobsFrequency())
                                      .build());
        }

        if (circumstances.getPension() == YesOrNo.YES) {
            regularIncome.addItem(RegularIncomeItemEntity.builder()
                                      .incomeType(IncomeType.PENSION)
                                      .amount(circumstances.getPensionAmount())
                                      .frequency(circumstances.getPensionFrequency())
                                      .build());
        }

        if (circumstances.getUniversalCreditAmount() != null) {
            regularIncome.addItem(RegularIncomeItemEntity.builder()
                                      .incomeType(IncomeType.UNIVERSAL_CREDIT)
                                      .amount(circumstances.getUniversalCreditAmount())
                                      .frequency(circumstances.getUniversalCreditFrequency())
                                      .build());
        }

        if (circumstances.getOtherBenefits() == YesOrNo.YES) {
            regularIncome.addItem(RegularIncomeItemEntity.builder()
                                      .incomeType(IncomeType.OTHER_BENEFITS)
                                      .amount(circumstances.getOtherBenefitsAmount())
                                      .frequency(circumstances.getOtherBenefitsFrequency())
                                      .build());
        }

        if (circumstances.getMoneyFromElsewhere() == YesOrNo.YES) {
            regularIncome.addItem(RegularIncomeItemEntity.builder()
                                      .incomeType(IncomeType.MONEY_FROM_ELSEWHERE)
                                      .build());
        }

        return regularIncome.getItems().isEmpty() ? null : regularIncome;
    }
}
