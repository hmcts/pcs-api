package uk.gov.hmcts.reform.pcs.ccd.service.respondpossessionclaim;

import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.IncomeType;
import uk.gov.hmcts.reform.pcs.ccd.domain.YesNoNotSure;
import uk.gov.hmcts.reform.pcs.ccd.domain.respondpossessionclaim.HouseholdCircumstances;
import uk.gov.hmcts.reform.pcs.ccd.entity.respondpossessionclaim.HouseholdCircumstancesEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.respondpossessionclaim.RegularIncomeEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.respondpossessionclaim.RegularIncomeItemEntity;

@Service
public class HouseholdCircumstancesService {

    public HouseholdCircumstancesEntity createHouseholdCircumstancesEntity(HouseholdCircumstances circumstances) {

        if (circumstances == null) {
            return null;
        }
        YesOrNo otherTenants = circumstances.getOtherTenants();
        YesNoNotSure alternativeAccommodation = circumstances.getAlternativeAccommodation();

        HouseholdCircumstancesEntity entity = HouseholdCircumstancesEntity.builder()
            .dependantChildren(circumstances.getDependantChildren())
            .shareAdditionalCircumstances(circumstances.getShareAdditionalCircumstances())
            .additionalCircumstancesDetails(circumstances.getAdditionalCircumstancesDetails())
            .exceptionalHardship(circumstances.getExceptionalHardship())
            .exceptionalHardshipDetails(circumstances.getExceptionalHardshipDetails())
            .dependantChildrenDetails(circumstances.getDependantChildrenDetails())
            .otherDependants(circumstances.getOtherDependants())
            .otherDependantDetails(circumstances.getOtherDependantDetails())
            .otherTenants(otherTenants)
            .otherTenantsDetails(otherTenants == YesOrNo.YES ? circumstances.getOtherTenantsDetails() : null)
            .alternativeAccommodation(alternativeAccommodation)
            .alternativeAccommodationTransferDate(alternativeAccommodation == YesNoNotSure.YES
                                                      ? circumstances.getAlternativeAccommodationTransferDate() : null)
            .shareIncomeExpenseDetails(circumstances.getShareIncomeExpenseDetails())
            .universalCredit(circumstances.getUniversalCredit())
            .ucApplicationDate(circumstances.getUcApplicationDate())
            .build();

        RegularIncomeEntity regularIncome = buildRegularIncome(circumstances);
        if (regularIncome != null) {
            entity.setRegularIncomeEntity(regularIncome);
        }

        return entity;
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
