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
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.YesNoNotSure;
import uk.gov.hmcts.reform.pcs.ccd.domain.respondpossessionclaim.HouseholdCircumstances;
import uk.gov.hmcts.reform.pcs.ccd.entity.respondpossessionclaim.HouseholdCircumstancesEntity;

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
    @EnumSource(VerticalYesNo.class)
    void shouldMapDependantChildrenField(VerticalYesNo expected) {
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
    void shouldMapOtherDependantsField(VerticalYesNo expected) {
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
            Arguments.of(VerticalYesNo.YES),
            Arguments.of(VerticalYesNo.NO),
            Arguments.of((VerticalYesNo) null)
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
    @EnumSource(VerticalYesNo.class)
    void shouldMapOtherTenantsField(VerticalYesNo expected) {
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
        VerticalYesNo expectedOtherTenants,
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
            Arguments.of(VerticalYesNo.YES, "Two other adults", "Two other adults"),
            Arguments.of(VerticalYesNo.YES, null, null),
            Arguments.of(VerticalYesNo.NO, "Draft still has text", null),
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
