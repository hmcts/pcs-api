package uk.gov.hmcts.reform.pcs.ccd.service.respondpossessionclaim;

import org.springframework.stereotype.Service;
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
            .shareIncomeExpenseDetails(circumstances.getShareIncomeExpenseDetails())
            .regularIncome(circumstances.getRegularIncome())
            .universalCredit(circumstances.getUniversalCredit())
            .ucApplicationDate(circumstances.getUcApplicationDate())
            .priorityDebts(circumstances.getPriorityDebts())
            .debtTotal(circumstances.getDebtTotal())
            .debtContribution(circumstances.getDebtContribution())
            .debtContributionFrequency(circumstances.getDebtContributionFrequency())
            .regularExpenses(circumstances.getRegularExpenses())
            .expenseAmount(circumstances.getExpenseAmount())
            .expenseFrequency(circumstances.getExpenseFrequency())
            .dependantChildrenDetails(circumstances.getDependantChildrenDetails())
            .otherDependants(circumstances.getOtherDependants())
            .otherDependantDetails(circumstances.getOtherDependantDetails())
            .build();
    }
}
