package uk.gov.hmcts.reform.pcs.ccd.service.respondpossessionclaim;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.respondpossessionclaim.HouseholdCircumstances;
import uk.gov.hmcts.reform.pcs.ccd.entity.respondpossessionclaim.HouseholdCircumstancesEntity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class HouseholdCircumstancesServiceTest {

    private HouseholdCircumstancesService underTest;

    @BeforeEach
    void setUp() {
        underTest = new HouseholdCircumstancesService();
    }

    @ParameterizedTest
    @MethodSource("dependantChildrenScenarios")
    void shouldMapDependantChildrenField(YesOrNo expected) {
        //Given
        HouseholdCircumstances householdCircumstances = HouseholdCircumstances.builder()
            .dependantChildren(expected)
            .build();

        //When
        HouseholdCircumstancesEntity entity = underTest.createHouseholdCircumstancesEntity(householdCircumstances);

        //Then
        assertThat(entity).isNotNull();
        assertThat(entity.getDependantChildren()).isEqualTo(expected);
    }

    private static Stream<Arguments> dependantChildrenScenarios() {
        return Stream.of(
            Arguments.of(YesOrNo.YES),
            Arguments.of(YesOrNo.NO),
            Arguments.of((YesOrNo) null)
        );
    }

    @Test
    void shouldMapDependantChildrenDetailsField() {
        //Given
        HouseholdCircumstances householdCircumstances = HouseholdCircumstances.builder()
            .dependantChildrenDetails("Two children aged 4 and 7")
            .build();

        //When
        HouseholdCircumstancesEntity entity = underTest.createHouseholdCircumstancesEntity(householdCircumstances);

        //Then
        assertThat(entity).isNotNull();
        assertThat(entity.getDependantChildrenDetails()).isEqualTo("Two children aged 4 and 7");
    }

    @ParameterizedTest
    @MethodSource("otherDependantsScenarios")
    void shouldMapOtherDependantsField(YesOrNo expected) {
        //Given
        HouseholdCircumstances householdCircumstances = HouseholdCircumstances.builder()
            .otherDependants(expected)
            .build();

        //When
        HouseholdCircumstancesEntity entity = underTest.createHouseholdCircumstancesEntity(householdCircumstances);

        //Then
        assertThat(entity).isNotNull();
        assertThat(entity.getOtherDependants()).isEqualTo(expected);
    }

    private static Stream<Arguments> otherDependantsScenarios() {
        return Stream.of(
            Arguments.of(YesOrNo.YES),
            Arguments.of(YesOrNo.NO),
            Arguments.of((YesOrNo) null)
        );
    }

    @Test
    void shouldMapOtherDependantDetailsField() {
        //Given
        HouseholdCircumstances householdCircumstances = HouseholdCircumstances.builder()
            .otherDependantDetails("Elderly parent requiring full-time care")
            .build();

        //When
        HouseholdCircumstancesEntity entity = underTest.createHouseholdCircumstancesEntity(householdCircumstances);

        //Then
        assertThat(entity).isNotNull();
        assertThat(entity.getOtherDependantDetails()).isEqualTo("Elderly parent requiring full-time care");
    }

    @Test
    void shouldReturnNullWhenHouseholdCircumstancesIsNull() {
        // When
        HouseholdCircumstancesEntity entity = underTest.createHouseholdCircumstancesEntity(null);

        // Then
        assertThat(entity).isNull();
    }

    @Test
    void shouldMapUniversalCreditAndPriorityDebtFields() {
        // Given
        HouseholdCircumstances householdCircumstances = HouseholdCircumstances.builder()
            .dependantChildren(YesOrNo.NO)
            .shareIncomeExpenseDetails(YesOrNo.YES)
            .regularIncome("INCOME_FROM_JOBS")
            .universalCredit(YesOrNo.YES)
            .ucApplicationDate(LocalDate.of(2024, 5, 12))
            .priorityDebts(YesOrNo.YES)
            .debtTotal(new BigDecimal("1500.00"))
            .debtContribution(new BigDecimal("200.00"))
            .debtContributionFrequency("MONTH")
            .regularExpenses("FOOD")
            .expenseAmount(new BigDecimal("300.00"))
            .expenseFrequency("WEEK")
            .build();

        // When
        HouseholdCircumstancesEntity entity = underTest.createHouseholdCircumstancesEntity(householdCircumstances);

        // Then
        assertThat(entity).isNotNull();
        assertThat(entity.getDependantChildren()).isEqualTo(YesOrNo.NO);
        assertThat(entity.getShareIncomeExpenseDetails()).isEqualTo(YesOrNo.YES);
        assertThat(entity.getRegularIncome()).isEqualTo("INCOME_FROM_JOBS");
        assertThat(entity.getUniversalCredit()).isEqualTo(YesOrNo.YES);
        assertThat(entity.getUcApplicationDate()).isEqualTo(LocalDate.of(2024, 5, 12));
        assertThat(entity.getPriorityDebts()).isEqualTo(YesOrNo.YES);
        assertThat(entity.getDebtTotal()).isEqualByComparingTo("1500.00");
        assertThat(entity.getDebtContribution()).isEqualByComparingTo("200.00");
        assertThat(entity.getDebtContributionFrequency()).isEqualTo("MONTH");
        assertThat(entity.getRegularExpenses()).isEqualTo("FOOD");
        assertThat(entity.getExpenseAmount()).isEqualByComparingTo("300.00");
        assertThat(entity.getExpenseFrequency()).isEqualTo("WEEK");
    }

}

