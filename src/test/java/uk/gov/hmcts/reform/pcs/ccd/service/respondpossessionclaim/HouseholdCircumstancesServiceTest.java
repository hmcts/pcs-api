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
import uk.gov.hmcts.reform.pcs.ccd.domain.YesNoNotSure;
import uk.gov.hmcts.reform.pcs.ccd.domain.respondpossessionclaim.HouseholdCircumstances;
import uk.gov.hmcts.reform.pcs.ccd.domain.respondpossessionclaim.IncomeExpenseDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.respondpossessionclaim.RecurrenceFrequency;
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
        assertThat(entity.getDependantChildren()).isEqualTo(expected);
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
    void shouldMapExpenseAmountsAndFrequenciesWhenAnswerIsYes() {
        HouseholdCircumstances householdCircumstances = buildExpenseFields(YesOrNo.YES);

        HouseholdCircumstancesEntity entity =
            underTest.createHouseholdCircumstancesEntity(householdCircumstances);

        assertThat(entity.getHouseholdBillsAmount()).isEqualByComparingTo(new BigDecimal("100.00"));
        assertThat(entity.getHouseholdBillsFrequency()).isEqualTo(RecurrenceFrequency.MONTHLY);
        assertThat(entity.getLoanPaymentsAmount()).isEqualByComparingTo(new BigDecimal("200.00"));
        assertThat(entity.getLoanPaymentsFrequency()).isEqualTo(RecurrenceFrequency.WEEKLY);
        assertThat(entity.getChildSpousalMaintenanceAmount()).isEqualByComparingTo(new BigDecimal("300.00"));
        assertThat(entity.getChildSpousalMaintenanceFrequency()).isEqualTo(RecurrenceFrequency.MONTHLY);
        assertThat(entity.getMobilePhoneAmount()).isEqualByComparingTo(new BigDecimal("400.00"));
        assertThat(entity.getMobilePhoneFrequency()).isEqualTo(RecurrenceFrequency.WEEKLY);
        assertThat(entity.getGroceryShoppingAmount()).isEqualByComparingTo(new BigDecimal("500.00"));
        assertThat(entity.getGroceryShoppingFrequency()).isEqualTo(RecurrenceFrequency.MONTHLY);
        assertThat(entity.getFuelParkingTransportAmount()).isEqualByComparingTo(new BigDecimal("600.00"));
        assertThat(entity.getFuelParkingTransportFrequency()).isEqualTo(RecurrenceFrequency.WEEKLY);
        assertThat(entity.getSchoolCostsAmount()).isEqualByComparingTo(new BigDecimal("700.00"));
        assertThat(entity.getSchoolCostsFrequency()).isEqualTo(RecurrenceFrequency.MONTHLY);
        assertThat(entity.getClothingAmount()).isEqualByComparingTo(new BigDecimal("800.00"));
        assertThat(entity.getClothingFrequency()).isEqualTo(RecurrenceFrequency.WEEKLY);
        assertThat(entity.getOtherExpensesAmount()).isEqualByComparingTo(new BigDecimal("900.00"));
        assertThat(entity.getOtherExpensesFrequency()).isEqualTo(RecurrenceFrequency.MONTHLY);
    }

    @ParameterizedTest
    @NullSource
    @EnumSource(value = YesOrNo.class, names = "NO")
    void shouldNotMapExpenseAmountsAndFrequenciesWhenAnswerIsNotYes(YesOrNo answer) {
        HouseholdCircumstances householdCircumstances = buildExpenseFields(answer);

        HouseholdCircumstancesEntity entity =
            underTest.createHouseholdCircumstancesEntity(householdCircumstances);

        assertThat(entity.getHouseholdBillsAmount()).isNull();
        assertThat(entity.getHouseholdBillsFrequency()).isNull();
        assertThat(entity.getLoanPaymentsAmount()).isNull();
        assertThat(entity.getLoanPaymentsFrequency()).isNull();
        assertThat(entity.getChildSpousalMaintenanceAmount()).isNull();
        assertThat(entity.getChildSpousalMaintenanceFrequency()).isNull();
        assertThat(entity.getMobilePhoneAmount()).isNull();
        assertThat(entity.getMobilePhoneFrequency()).isNull();
        assertThat(entity.getGroceryShoppingAmount()).isNull();
        assertThat(entity.getGroceryShoppingFrequency()).isNull();
        assertThat(entity.getFuelParkingTransportAmount()).isNull();
        assertThat(entity.getFuelParkingTransportFrequency()).isNull();
        assertThat(entity.getSchoolCostsAmount()).isNull();
        assertThat(entity.getSchoolCostsFrequency()).isNull();
        assertThat(entity.getClothingAmount()).isNull();
        assertThat(entity.getClothingFrequency()).isNull();
        assertThat(entity.getOtherExpensesAmount()).isNull();
        assertThat(entity.getOtherExpensesFrequency()).isNull();
    }

    private static HouseholdCircumstances buildExpenseFields(YesOrNo answer) {
        return HouseholdCircumstances.builder()
            .householdBills(buildExpense(answer, "100.00", RecurrenceFrequency.MONTHLY))
            .loanPayments(buildExpense(answer, "200.00", RecurrenceFrequency.WEEKLY))
            .childSpousalMaintenance(buildExpense(answer, "300.00", RecurrenceFrequency.MONTHLY))
            .mobilePhone(buildExpense(answer, "400.00", RecurrenceFrequency.WEEKLY))
            .groceryShopping(buildExpense(answer, "500.00", RecurrenceFrequency.MONTHLY))
            .fuelParkingTransport(buildExpense(answer, "600.00", RecurrenceFrequency.WEEKLY))
            .schoolCosts(buildExpense(answer, "700.00", RecurrenceFrequency.MONTHLY))
            .clothing(buildExpense(answer, "800.00", RecurrenceFrequency.WEEKLY))
            .otherExpenses(buildExpense(answer, "900.00", RecurrenceFrequency.MONTHLY))
            .build();
    }

    private static IncomeExpenseDetails buildExpense(YesOrNo applies, String amount, RecurrenceFrequency frequency) {
        return IncomeExpenseDetails.builder()
            .applies(applies)
            .amount(new BigDecimal(amount))
            .frequency(frequency)
            .build();
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
        assertThat(entity.getOtherTenants()).isEqualTo(expected);
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
        assertThat(entity.getOtherTenants()).isEqualTo(expectedOtherTenants);
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
}
