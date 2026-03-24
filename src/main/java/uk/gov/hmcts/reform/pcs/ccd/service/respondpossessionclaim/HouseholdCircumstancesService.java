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

        HouseholdCircumstancesEntity householdCircumstancesEntity = HouseholdCircumstancesEntity.builder()
            .dependantChildren(circumstances.getDependantChildren())
            .shareIncomeExpenseDetails(circumstances.getShareIncomeExpenseDetails())
            .incomeFromJobs(circumstances.getIncomeFromJobs())
            .incomeFromJobsAmount(circumstances.getIncomeFromJobsAmount())
            .incomeFromJobsFrequency(circumstances.getIncomeFromJobsFrequency())
            .pension(circumstances.getPension())
            .pensionAmount(circumstances.getPensionAmount())
            .pensionFrequency(circumstances.getPensionFrequency())
            .universalCredit(circumstances.getUniversalCredit())
            .ucApplicationDate(circumstances.getUcApplicationDate())
            .universalCreditAmount(circumstances.getUniversalCreditAmount())
            .universalCreditFrequency(circumstances.getUniversalCreditFrequency())
            .otherBenefits(circumstances.getOtherBenefits())
            .otherBenefitsAmount(circumstances.getOtherBenefitsAmount())
            .otherBenefitsFrequency(circumstances.getOtherBenefitsFrequency())
            .moneyFromElsewhere(circumstances.getMoneyFromElsewhere())
            .moneyFromElsewhereDetails(circumstances.getMoneyFromElsewhereDetails())
            .build();

        return householdCircumstancesEntity;
    }
}
