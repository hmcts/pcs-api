package uk.gov.hmcts.reform.pcs.ccd.service.respondpossessionclaim;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullSource;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.IncomeType;
import uk.gov.hmcts.reform.pcs.ccd.domain.RecurrenceFrequency;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.YesNoNotSure;
import uk.gov.hmcts.reform.pcs.ccd.domain.respondpossessionclaim.HouseholdCircumstances;
import uk.gov.hmcts.reform.pcs.ccd.entity.respondpossessionclaim.HouseholdCircumstancesEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.respondpossessionclaim.RegularIncomeItemEntity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
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
    @NullSource
    @EnumSource(YesOrNo.class)
    void shouldMapDependantChildrenField(YesOrNo expected) {
        // Given
        HouseholdCircumstances householdCircumstances = HouseholdCircumstances.builder()
            .dependantChildren(expected)
            .build();

        // When
        HouseholdCircumstancesEntity entity = underTest.createHouseholdCircumstancesEntity(householdCircumstances);

        // Then
        assertThat(entity.getDependantChildren()).isEqualTo(toVerticalYesNo(expected));
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
        assertThat(entity.getOtherDependants()).isEqualTo(toVerticalYesNo(expected));
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

    @ParameterizedTest
    @NullSource
    @EnumSource(YesOrNo.class)
    void shouldMapOtherTenantsField(YesOrNo expected) {
        // Given
        HouseholdCircumstances householdCircumstances = HouseholdCircumstances.builder()
            .otherTenants(expected)
            .build();

        // When
        HouseholdCircumstancesEntity entity = underTest.createHouseholdCircumstancesEntity(householdCircumstances);

        // Then
        assertThat(entity.getOtherTenants()).isEqualTo(toVerticalYesNo(expected));
    }

    @ParameterizedTest
    @MethodSource("otherTenantsDetailsScenarios")
    void shouldMapOtherTenantsDetailsOnlyWhenOtherTenantsIsYes(
        YesOrNo expectedOtherTenants,
        String draftDetails,
        String expectedDetailsOnEntity
    ) {
        // Given
        HouseholdCircumstances householdCircumstances = HouseholdCircumstances.builder()
            .otherTenants(expectedOtherTenants)
            .otherTenantsDetails(draftDetails)
            .build();

        // When
        HouseholdCircumstancesEntity entity = underTest.createHouseholdCircumstancesEntity(householdCircumstances);

        // Then
        assertThat(entity.getOtherTenants()).isEqualTo(toVerticalYesNo(expectedOtherTenants));
        assertThat(entity.getOtherTenantsDetails()).isEqualTo(expectedDetailsOnEntity);
    }

    private static Stream<Arguments> otherTenantsDetailsScenarios() {
        return Stream.of(
            Arguments.of(YesOrNo.YES, "Two other adults", "Two other adults"),
            Arguments.of(YesOrNo.YES, null, null),
            Arguments.of(YesOrNo.NO, "Draft still has text", null),
            Arguments.of(null, "Draft still has text", null)
        );
    }

    @ParameterizedTest
    @NullSource
    @EnumSource(YesNoNotSure.class)
    void shouldMapAlternativeAccommodationField(YesNoNotSure expected) {
        // Given
        HouseholdCircumstances householdCircumstances = HouseholdCircumstances.builder()
            .alternativeAccommodation(expected)
            .build();

        // When
        HouseholdCircumstancesEntity entity = underTest.createHouseholdCircumstancesEntity(householdCircumstances);

        // Then
        assertThat(entity.getAlternativeAccommodation()).isEqualTo(expected);
    }

    @ParameterizedTest
    @MethodSource("alternativeAccommodationTransferDateScenarios")
    void shouldMapAlternativeAccommodationTransferDateAccordingToAccommodationAnswer(
        YesNoNotSure expectedAlternativeAccommodation,
        LocalDate draftDate,
        LocalDate expectedDate
    ) {
        // Given
        HouseholdCircumstances householdCircumstances = HouseholdCircumstances.builder()
            .alternativeAccommodation(expectedAlternativeAccommodation)
            .alternativeAccommodationTransferDate(draftDate)
            .build();

        // When
        HouseholdCircumstancesEntity entity = underTest.createHouseholdCircumstancesEntity(householdCircumstances);

        // Then
        assertThat(entity.getAlternativeAccommodation()).isEqualTo(expectedAlternativeAccommodation);
        assertThat(entity.getAlternativeAccommodationTransferDate()).isEqualTo(expectedDate);
    }

    private static Stream<Arguments> alternativeAccommodationTransferDateScenarios() {
        LocalDate transferDate = LocalDate.of(2025, 6, 1);
        return Stream.of(
            Arguments.of(YesNoNotSure.YES, transferDate, transferDate),
            Arguments.of(YesNoNotSure.YES, null, null),
            Arguments.of(YesNoNotSure.NO, transferDate, null),
            Arguments.of(YesNoNotSure.NOT_SURE, transferDate, null),
            Arguments.of(null, transferDate, null)
        );
    }

    @Test
    void shouldCreateIncomeFromJobsItem() {
        HouseholdCircumstances circumstances = HouseholdCircumstances.builder()
            .incomeFromJobs(YesOrNo.YES)
            .incomeFromJobsAmount(new BigDecimal("200000"))
            .incomeFromJobsFrequency(RecurrenceFrequency.MONTHLY)
            .build();

        HouseholdCircumstancesEntity entity = underTest.createHouseholdCircumstancesEntity(circumstances);

        assertThat(entity.getRegularIncomeEntity()).isNotNull();
        List<RegularIncomeItemEntity> items = entity.getRegularIncomeEntity().getItems();
        assertThat(items).hasSize(1);
        assertThat(items.get(0).getIncomeType()).isEqualTo(IncomeType.INCOME_FROM_JOBS);
        assertThat(items.get(0).getAmount()).isEqualByComparingTo("200000");
        assertThat(items.get(0).getFrequency()).isEqualTo(RecurrenceFrequency.MONTHLY);
    }

    @Test
    void shouldNotCreateIncomeFromJobsItemWhenNo() {
        HouseholdCircumstances circumstances = HouseholdCircumstances.builder()
            .incomeFromJobs(YesOrNo.NO)
            .build();

        HouseholdCircumstancesEntity entity = underTest.createHouseholdCircumstancesEntity(circumstances);

        assertThat(entity.getRegularIncomeEntity()).isNull();
    }

    @Test
    void shouldCreatePensionItem() {
        HouseholdCircumstances circumstances = HouseholdCircumstances.builder()
            .pension(YesOrNo.YES)
            .pensionAmount(new BigDecimal("50000"))
            .pensionFrequency(RecurrenceFrequency.MONTHLY)
            .build();

        HouseholdCircumstancesEntity entity = underTest.createHouseholdCircumstancesEntity(circumstances);

        assertThat(entity.getRegularIncomeEntity()).isNotNull();
        List<RegularIncomeItemEntity> items = entity.getRegularIncomeEntity().getItems();
        assertThat(items).hasSize(1);
        assertThat(items.get(0).getIncomeType()).isEqualTo(IncomeType.PENSION);
        assertThat(items.get(0).getAmount()).isEqualByComparingTo("50000");
        assertThat(items.get(0).getFrequency()).isEqualTo(RecurrenceFrequency.MONTHLY);
    }

    @Test
    void shouldCreateUniversalCreditItemWhenAmountProvided() {
        HouseholdCircumstances circumstances = HouseholdCircumstances.builder()
            .universalCreditAmount(new BigDecimal("100000"))
            .universalCreditFrequency(RecurrenceFrequency.MONTHLY)
            .build();

        HouseholdCircumstancesEntity entity = underTest.createHouseholdCircumstancesEntity(circumstances);

        assertThat(entity.getRegularIncomeEntity()).isNotNull();
        List<RegularIncomeItemEntity> items = entity.getRegularIncomeEntity().getItems();
        assertThat(items).hasSize(1);
        assertThat(items.get(0).getIncomeType()).isEqualTo(IncomeType.UNIVERSAL_CREDIT);
        assertThat(items.get(0).getAmount()).isEqualByComparingTo("100000");
        assertThat(items.get(0).getFrequency()).isEqualTo(RecurrenceFrequency.MONTHLY);
    }

    @Test
    void shouldNotCreateUniversalCreditItemWhenAmountNull() {
        HouseholdCircumstances circumstances = HouseholdCircumstances.builder()
            .universalCredit(YesOrNo.YES)
            .build();

        HouseholdCircumstancesEntity entity = underTest.createHouseholdCircumstancesEntity(circumstances);

        assertThat(entity.getRegularIncomeEntity()).isNull();
    }

    @Test
    void shouldCreateOtherBenefitsItem() {
        HouseholdCircumstances circumstances = HouseholdCircumstances.builder()
            .otherBenefits(YesOrNo.YES)
            .otherBenefitsAmount(new BigDecimal("20000"))
            .otherBenefitsFrequency(RecurrenceFrequency.WEEKLY)
            .build();

        HouseholdCircumstancesEntity entity = underTest.createHouseholdCircumstancesEntity(circumstances);

        assertThat(entity.getRegularIncomeEntity()).isNotNull();
        List<RegularIncomeItemEntity> items = entity.getRegularIncomeEntity().getItems();
        assertThat(items).hasSize(1);
        assertThat(items.get(0).getIncomeType()).isEqualTo(IncomeType.OTHER_BENEFITS);
        assertThat(items.get(0).getAmount()).isEqualByComparingTo("20000");
        assertThat(items.get(0).getFrequency()).isEqualTo(RecurrenceFrequency.WEEKLY);
    }

    @Test
    void shouldCreateMoneyFromElsewhereItemWithDetails() {
        HouseholdCircumstances circumstances = HouseholdCircumstances.builder()
            .moneyFromElsewhere(YesOrNo.YES)
            .moneyFromElsewhereDetails("Child maintenance payments")
            .build();

        HouseholdCircumstancesEntity entity = underTest.createHouseholdCircumstancesEntity(circumstances);

        assertThat(entity.getRegularIncomeEntity()).isNotNull();
        List<RegularIncomeItemEntity> items = entity.getRegularIncomeEntity().getItems();
        assertThat(items).hasSize(1);
        assertThat(items.get(0).getIncomeType()).isEqualTo(IncomeType.MONEY_FROM_ELSEWHERE);
        assertThat(items.get(0).getAmount()).isNull();
        assertThat(entity.getRegularIncomeEntity().getOtherIncomeDetails()).isEqualTo("Child maintenance payments");
    }

    @Test
    void shouldCreateMultipleIncomeItems() {
        HouseholdCircumstances circumstances = HouseholdCircumstances.builder()
            .incomeFromJobs(YesOrNo.YES)
            .incomeFromJobsAmount(new BigDecimal("200000"))
            .incomeFromJobsFrequency(RecurrenceFrequency.MONTHLY)
            .pension(YesOrNo.YES)
            .pensionAmount(new BigDecimal("50000"))
            .pensionFrequency(RecurrenceFrequency.MONTHLY)
            .universalCreditAmount(new BigDecimal("100000"))
            .universalCreditFrequency(RecurrenceFrequency.MONTHLY)
            .otherBenefits(YesOrNo.YES)
            .otherBenefitsAmount(new BigDecimal("20000"))
            .otherBenefitsFrequency(RecurrenceFrequency.WEEKLY)
            .moneyFromElsewhere(YesOrNo.YES)
            .moneyFromElsewhereDetails("Child support")
            .build();

        HouseholdCircumstancesEntity entity = underTest.createHouseholdCircumstancesEntity(circumstances);

        assertThat(entity.getRegularIncomeEntity()).isNotNull();
        List<RegularIncomeItemEntity> items = entity.getRegularIncomeEntity().getItems();
        assertThat(items).hasSize(5);
        assertThat(items).extracting(RegularIncomeItemEntity::getIncomeType)
            .containsExactly(
                IncomeType.INCOME_FROM_JOBS,
                IncomeType.PENSION,
                IncomeType.UNIVERSAL_CREDIT,
                IncomeType.OTHER_BENEFITS,
                IncomeType.MONEY_FROM_ELSEWHERE
        );
    }

    @Test
    void shouldNotPersistDetailsWhenMoneyFromElsewhereIsNo() {
        HouseholdCircumstances circumstances = HouseholdCircumstances.builder()
            .incomeFromJobs(YesOrNo.YES)
            .incomeFromJobsAmount(new BigDecimal("200000"))
            .incomeFromJobsFrequency(RecurrenceFrequency.MONTHLY)
            .moneyFromElsewhere(YesOrNo.NO)
            .moneyFromElsewhereDetails("Stale draft text")
            .build();

        HouseholdCircumstancesEntity entity = underTest.createHouseholdCircumstancesEntity(circumstances);

        assertThat(entity.getRegularIncomeEntity()).isNotNull();
        assertThat(entity.getRegularIncomeEntity().getOtherIncomeDetails()).isNull();
    }

    @Test
    void shouldNotCreateRegularIncomeWhenNoIncomeTypesSelected() {
        HouseholdCircumstances circumstances = HouseholdCircumstances.builder()
            .incomeFromJobs(YesOrNo.NO)
            .pension(YesOrNo.NO)
            .otherBenefits(YesOrNo.NO)
            .moneyFromElsewhere(YesOrNo.NO)
            .build();

        HouseholdCircumstancesEntity entity = underTest.createHouseholdCircumstancesEntity(circumstances);

        assertThat(entity.getRegularIncomeEntity()).isNull();
    }

    private VerticalYesNo toVerticalYesNo(YesOrNo expected) {
        if (expected == null) {
            return null;
        }
        return expected == YesOrNo.YES ? VerticalYesNo.YES : VerticalYesNo.NO;
    }
}
