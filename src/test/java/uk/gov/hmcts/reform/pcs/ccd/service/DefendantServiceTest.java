package uk.gov.hmcts.reform.pcs.ccd.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;
import uk.gov.hmcts.ccd.sdk.type.AddressUK;
import uk.gov.hmcts.ccd.sdk.type.ListValue;
import uk.gov.hmcts.reform.pcs.ccd.domain.DefendantDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.model.Defendant;
import uk.gov.hmcts.reform.pcs.ccd.type.DynamicStringListElement;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.junit.jupiter.params.ParameterizedTest.ARGUMENT_SET_NAME_PLACEHOLDER;
import static org.junit.jupiter.params.provider.Arguments.argumentSet;
import static org.mockito.Mock.Strictness.LENIENT;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.pcs.ccd.util.ListValueUtils.wrapListItems;

@ExtendWith(MockitoExtension.class)
class DefendantServiceTest {

    @Mock
    private ModelMapper modelMapper;

    @Mock(strictness = LENIENT)
    private PCSCase pcsCase;

    private DefendantService underTest;

    @BeforeEach
    void setUp() {
        underTest = new DefendantService(modelMapper);
    }

    @Test
    void shouldThrowExceptionForNullDefendant1() {
        // Given
        when(pcsCase.getDefendant1()).thenReturn(null);

        // When
        Throwable throwable = catchThrowable(() -> underTest.buildDefendantsList(pcsCase));

        // Then
        assertThat(throwable)
            .isInstanceOf(NullPointerException.class)
            .hasMessage("Defendant 1 must be provided");
    }

    @ParameterizedTest(name = ARGUMENT_SET_NAME_PLACEHOLDER)
    @MethodSource("singleDefendantScenarios")
    void shouldBuildListWithSingleDefendant(DefendantDetails defendant1Details, Defendant expectedDefendant) {
        // Given
        when(pcsCase.getDefendant1()).thenReturn(defendant1Details);
        when(pcsCase.getAddAnotherDefendant()).thenReturn(VerticalYesNo.NO);

        // When
        List<Defendant> defendantList = underTest.buildDefendantsList(pcsCase);

        expectedDefendant.setPartyId(defendantList.getFirst().getPartyId());

        // Then
        assertThat(defendantList).containsExactly(expectedDefendant);
    }

    @Test
    void shouldBuildListWithMultipleDefendants() {
        // Given
        AddressUK defendant1Address = mock(AddressUK.class);

        DefendantDetails defendant1Details = DefendantDetails.builder()
            .nameKnown(VerticalYesNo.YES)
            .firstName("defendant 1 first name")
            .lastName(("defendant 1 last name"))
            .addressKnown(VerticalYesNo.YES)
            .addressSameAsPossession(VerticalYesNo.NO)
            .correspondenceAddress(defendant1Address)
            .build();

        DefendantDetails defendant2Details = DefendantDetails.builder()
            .nameKnown(VerticalYesNo.YES)
            .firstName("defendant 2 first name")
            .lastName(("defendant 2 last name"))
            .addressKnown(VerticalYesNo.NO)
            .build();

        DefendantDetails defendant3Details = DefendantDetails.builder()
            .nameKnown(VerticalYesNo.NO)
            .addressKnown(VerticalYesNo.YES)
            .addressSameAsPossession(VerticalYesNo.YES)
            .build();

        when(pcsCase.getDefendant1()).thenReturn(defendant1Details);

        List<DefendantDetails> additionalDefendantsDetails = List.of(defendant2Details, defendant3Details);
        when(pcsCase.getAdditionalDefendants()).thenReturn(wrapListItems(additionalDefendantsDetails));
        when(pcsCase.getAddAnotherDefendant()).thenReturn(VerticalYesNo.YES);

        // When
        List<Defendant> defendantList = underTest.buildDefendantsList(pcsCase);

        // Then
        Defendant expectedDefendant1 = Defendant.builder()
            .partyId(defendantList.getFirst().getPartyId())
            .nameKnown(true)
            .firstName("defendant 1 first name")
            .lastName("defendant 1 last name")
            .addressKnown(true)
            .addressSameAsPossession(false)
            .correspondenceAddress(defendant1Address)
            .additionalDefendantsAdded(true)
            .build();

        Defendant expectedDefendant2 = Defendant.builder()
            .partyId(defendantList.get(1).getPartyId())
            .nameKnown(true)
            .firstName("defendant 2 first name")
            .lastName("defendant 2 last name")
            .addressKnown(false)
            .build();

        Defendant expectedDefendant3 = Defendant.builder()
            .partyId(defendantList.get(2).getPartyId())
            .nameKnown(false)
            .addressKnown(true)
            .addressSameAsPossession(true)
            .build();

        assertThat(defendantList).containsExactly(expectedDefendant1, expectedDefendant2, expectedDefendant3);
    }

    @Test
    void shouldIgnoreMultipleDefendantsIfAdditionalDefendantsNotIndicated() {
        // Given
        AddressUK defendant1Address = mock(AddressUK.class);

        DefendantDetails defendant1Details = DefendantDetails.builder()
            .nameKnown(VerticalYesNo.YES)
            .firstName("defendant 1 first name")
            .lastName(("defendant 1 last name"))
            .addressKnown(VerticalYesNo.YES)
            .addressSameAsPossession(VerticalYesNo.NO)
            .correspondenceAddress(defendant1Address)
            .build();

        DefendantDetails defendant2Details = DefendantDetails.builder()
            .nameKnown(VerticalYesNo.YES)
            .firstName("defendant 2 first name")
            .lastName(("defendant 2 last name"))
            .addressKnown(VerticalYesNo.NO)
            .build();

        when(pcsCase.getDefendant1()).thenReturn(defendant1Details);
        when(pcsCase.getAdditionalDefendants()).thenReturn(wrapListItems(List.of(defendant2Details)));
        when(pcsCase.getAddAnotherDefendant()).thenReturn(VerticalYesNo.NO);

        // When
        List<Defendant> defendantList = underTest.buildDefendantsList(pcsCase);

        // Then
        Defendant expectedDefendant1 = Defendant.builder()
            .partyId(defendantList.getFirst().getPartyId())
            .nameKnown(true)
            .firstName("defendant 1 first name")
            .lastName("defendant 1 last name")
            .addressKnown(true)
            .addressSameAsPossession(false)
            .correspondenceAddress(defendant1Address)
            .additionalDefendantsAdded(false)
            .build();

        assertThat(defendantList).containsExactly(expectedDefendant1);
    }

    @Test
    void shouldIgnoreNullAdditionalDefendantsEvenIfAdditionalIndicated() {
        // Given
        DefendantDetails defendant1Details = DefendantDetails.builder()
            .nameKnown(VerticalYesNo.YES)
            .firstName("defendant 1 first name")
            .lastName(("defendant 1 last name"))
            .addressKnown(VerticalYesNo.NO)
            .build();

        when(pcsCase.getDefendant1()).thenReturn(defendant1Details);

        when(pcsCase.getAdditionalDefendants()).thenReturn(null);
        when(pcsCase.getAddAnotherDefendant()).thenReturn(VerticalYesNo.YES);

        // When
        List<Defendant> defendantList = underTest.buildDefendantsList(pcsCase);

        // Then
        Defendant expectedDefendant1 = Defendant.builder()
            .partyId(defendantList.get(0).getPartyId())
            .nameKnown(true)
            .firstName("defendant 1 first name")
            .lastName("defendant 1 last name")
            .addressKnown(false)
            .additionalDefendantsAdded(true)
            .build();

        assertThat(defendantList).containsExactly(expectedDefendant1);
    }

    @Test
    void shouldBlankFieldsIfNotKnownSelected() {
        // Given
        AddressUK correspondenceAddress = mock(AddressUK.class);

        DefendantDetails defendantDetails = DefendantDetails.builder()
            .nameKnown(VerticalYesNo.NO)
            .firstName("should be ignored")
            .lastName(("should be ignored"))
            .addressKnown(VerticalYesNo.NO)
            .addressSameAsPossession(VerticalYesNo.NO)
            .correspondenceAddress(correspondenceAddress)
            .build();

        DefendantDetails additionalDefendantDetails = DefendantDetails.builder()
            .nameKnown(VerticalYesNo.NO)
            .firstName("should be ignored")
            .lastName(("should be ignored"))
            .addressKnown(VerticalYesNo.NO)
            .addressSameAsPossession(VerticalYesNo.NO)
            .correspondenceAddress(correspondenceAddress)
            .build();

        when(pcsCase.getDefendant1()).thenReturn(defendantDetails);
        when(pcsCase.getAdditionalDefendants()).thenReturn(wrapListItems(List.of(additionalDefendantDetails)));
        when(pcsCase.getAddAnotherDefendant()).thenReturn(VerticalYesNo.YES);

        // When
        List<Defendant> defendantList = underTest.buildDefendantsList(pcsCase);

        // Then
        Defendant expectedDefendant1 = Defendant.builder()
            .partyId(defendantList.getFirst().getPartyId())
            .nameKnown(false)
            .addressKnown(false)
            .additionalDefendantsAdded(true)
            .build();

        Defendant expectedDefendant2 = Defendant.builder()
            .partyId(defendantList.get(1).getPartyId())
            .nameKnown(false)
            .addressKnown(false)
            .build();

        assertThat(defendantList).containsExactly(expectedDefendant1, expectedDefendant2);
    }

    @Test
    void shouldMapFromNullDefendantListToEmptyDefendantDetailsList() {
        // When
        List<DefendantDetails> actualDefendantDetails = underTest.mapToDefendantDetails(null);

        // Then
        assertThat(actualDefendantDetails).isEmpty();
    }

    @Test
    void shouldMapFromDefendantListToDefendantDetailsList() {
        // Given
        Defendant defendant1 = mock(Defendant.class);
        Defendant defendant2 = mock(Defendant.class);

        DefendantDetails defendantDetails1 = mock(DefendantDetails.class);
        DefendantDetails defendantDetails2 = mock(DefendantDetails.class);

        when(modelMapper.map(defendant1, DefendantDetails.class)).thenReturn(defendantDetails1);
        when(modelMapper.map(defendant2, DefendantDetails.class)).thenReturn(defendantDetails2);

        // When
        List<DefendantDetails> actualDefendantDetails
            = underTest.mapToDefendantDetails(List.of(defendant1, defendant2));

        // Then
        assertThat(actualDefendantDetails).containsExactly(defendantDetails1, defendantDetails2);
    }

    @Nested
    @DisplayName("buildDefendantDisplayName tests")
    class BuildDefendantDisplayNameTests {

        @Test
        @DisplayName("Should return 'Unknown' when defendant details is null")
        void shouldReturnUnknownWhenDefendantDetailsIsNull() {
            // When
            String result = underTest.buildDefendantDisplayName(null);

            // Then
            assertThat(result).isEqualTo("Unknown");
        }

        @Test
        @DisplayName("Should return 'Name not known' when nameKnown is NO")
        void shouldReturnNameNotKnownWhenNameKnownIsNo() {
            // Given
            DefendantDetails details = DefendantDetails.builder()
                .nameKnown(VerticalYesNo.NO)
                .firstName("John")
                .lastName("Doe")
                .build();

            // When
            String result = underTest.buildDefendantDisplayName(details);

            // Then
            assertThat(result).isEqualTo("Name not known");
        }

        @ParameterizedTest(name = ARGUMENT_SET_NAME_PLACEHOLDER)
        @MethodSource("defendantDisplayNameScenarios")
        @DisplayName("Should return correct display name for various name combinations")
        void shouldReturnCorrectDisplayNameForVariousNameCombinations(
            DefendantDetails details, String expectedDisplayName) {
            // When
            String result = underTest.buildDefendantDisplayName(details);

            // Then
            assertThat(result).isEqualTo(expectedDisplayName);
        }

        @Test
        @DisplayName("Should trim whitespace from final result")
        void shouldTrimWhitespaceFromFinalResult() {
            // Given
            DefendantDetails details = DefendantDetails.builder()
                .nameKnown(VerticalYesNo.YES)
                .firstName("  John  ")
                .lastName("  Doe  ")
                .build();

            // When
            String result = underTest.buildDefendantDisplayName(details);

            // Then
            // Note: The method trims the final result but not individual names,
            // so spaces within names are preserved but leading/trailing spaces are removed
            assertThat(result).isEqualTo("  John     Doe  ".trim());
            assertThat(result).isEqualTo("John     Doe");
        }

        private static Stream<Arguments> defendantDisplayNameScenarios() {
            return Stream.of(
                argumentSet(
                    "Full name when both first and last name are provided",
                    DefendantDetails.builder()
                        .nameKnown(VerticalYesNo.YES)
                        .firstName("John")
                        .lastName("Doe")
                        .build(),
                    "John Doe"
                ),
                argumentSet(
                    "First name only when last name is null",
                    DefendantDetails.builder()
                        .nameKnown(VerticalYesNo.YES)
                        .firstName("John")
                        .lastName(null)
                        .build(),
                    "John"
                ),
                argumentSet(
                    "Last name only when first name is null",
                    DefendantDetails.builder()
                        .nameKnown(VerticalYesNo.YES)
                        .firstName(null)
                        .lastName("Doe")
                        .build(),
                    "Doe"
                ),
                argumentSet(
                    "Unknown when both names are empty strings",
                    DefendantDetails.builder()
                        .nameKnown(VerticalYesNo.YES)
                        .firstName("")
                        .lastName("")
                        .build(),
                    "Unknown"
                ),
                argumentSet(
                    "Unknown when both names are null",
                    DefendantDetails.builder()
                        .nameKnown(VerticalYesNo.YES)
                        .firstName(null)
                        .lastName(null)
                        .build(),
                    "Unknown"
                )
            );
        }
    }

    @Nested
    @DisplayName("buildDefendantListItems tests")
    class BuildDefendantListItemsTests {

        @Test
        @DisplayName("Should return empty list when allDefendants is null")
        void shouldReturnEmptyListWhenAllDefendantsIsNull() {
            // When
            List<DynamicStringListElement> result = underTest.buildDefendantListItems(null);

            // Then
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("Should return empty list when allDefendants is empty")
        void shouldReturnEmptyListWhenAllDefendantsIsEmpty() {
            // When
            List<DynamicStringListElement> result = underTest.buildDefendantListItems(new ArrayList<>());

            // Then
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("Should build list with single defendant")
        void shouldBuildListWithSingleDefendant() {
            // Given
            DefendantDetails defendantDetails = DefendantDetails.builder()
                .nameKnown(VerticalYesNo.YES)
                .firstName("John")
                .lastName("Doe")
                .build();

            List<ListValue<DefendantDetails>> allDefendants = List.of(
                ListValue.<DefendantDetails>builder().value(defendantDetails).build()
            );

            // When
            List<DynamicStringListElement> result = underTest.buildDefendantListItems(allDefendants);

            // Then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getCode()).matches(Pattern.compile(
                "^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$",
                Pattern.CASE_INSENSITIVE));
            assertThat(result.get(0).getLabel()).isEqualTo("John Doe");
        }

        @Test
        @DisplayName("Should build list with multiple defendants")
        void shouldBuildListWithMultipleDefendants() {
            // Given
            DefendantDetails defendant1 = DefendantDetails.builder()
                .nameKnown(VerticalYesNo.YES)
                .firstName("John")
                .lastName("Doe")
                .build();

            DefendantDetails defendant2 = DefendantDetails.builder()
                .nameKnown(VerticalYesNo.YES)
                .firstName("Jane")
                .lastName("Smith")
                .build();

            DefendantDetails defendant3 = DefendantDetails.builder()
                .nameKnown(VerticalYesNo.NO)
                .build();

            List<ListValue<DefendantDetails>> allDefendants = List.of(
                ListValue.<DefendantDetails>builder().value(defendant1).build(),
                ListValue.<DefendantDetails>builder().value(defendant2).build(),
                ListValue.<DefendantDetails>builder().value(defendant3).build()
            );

            // When
            List<DynamicStringListElement> result = underTest.buildDefendantListItems(allDefendants);

            // Then
            assertThat(result).hasSize(3);
            Pattern uuidPattern = Pattern.compile(
                "^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$",
                Pattern.CASE_INSENSITIVE);
            assertThat(result.get(0).getCode()).matches(uuidPattern);
            assertThat(result.get(0).getLabel()).isEqualTo("John Doe");
            assertThat(result.get(1).getCode()).matches(uuidPattern);
            assertThat(result.get(1).getLabel()).isEqualTo("Jane Smith");
            assertThat(result.get(2).getCode()).matches(uuidPattern);
            assertThat(result.get(2).getLabel()).isEqualTo("Name not known");
            // Ensure all codes are unique
            assertThat(result.stream().map(DynamicStringListElement::getCode).distinct())
                .hasSize(3);
        }

        @Test
        @DisplayName("Should handle mixed scenarios with various name combinations")
        void shouldHandleMixedScenariosWithVariousNameCombinations() {
            // Given
            DefendantDetails defendant1 = DefendantDetails.builder()
                .nameKnown(VerticalYesNo.YES)
                .firstName("John")
                .lastName("Doe")
                .build();

            DefendantDetails defendant2 = DefendantDetails.builder()
                .nameKnown(VerticalYesNo.YES)
                .firstName("Jane")
                .lastName(null)
                .build();

            DefendantDetails defendant3 = DefendantDetails.builder()
                .nameKnown(VerticalYesNo.YES)
                .firstName(null)
                .lastName("Smith")
                .build();

            DefendantDetails defendant4 = DefendantDetails.builder()
                .nameKnown(VerticalYesNo.YES)
                .firstName("")
                .lastName("")
                .build();

            DefendantDetails defendant5 = DefendantDetails.builder()
                .nameKnown(VerticalYesNo.NO)
                .build();

            List<ListValue<DefendantDetails>> allDefendants = List.of(
                ListValue.<DefendantDetails>builder().value(defendant1).build(),
                ListValue.<DefendantDetails>builder().value(defendant2).build(),
                ListValue.<DefendantDetails>builder().value(defendant3).build(),
                ListValue.<DefendantDetails>builder().value(defendant4).build(),
                ListValue.<DefendantDetails>builder().value(defendant5).build()
            );

            // When
            List<DynamicStringListElement> result = underTest.buildDefendantListItems(allDefendants);

            // Then
            assertThat(result).hasSize(5);
            Pattern uuidPattern = Pattern.compile(
                "^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$",
                Pattern.CASE_INSENSITIVE);
            assertThat(result.get(0).getCode()).matches(uuidPattern);
            assertThat(result.get(0).getLabel()).isEqualTo("John Doe");
            assertThat(result.get(1).getCode()).matches(uuidPattern);
            assertThat(result.get(1).getLabel()).isEqualTo("Jane");
            assertThat(result.get(2).getCode()).matches(uuidPattern);
            assertThat(result.get(2).getLabel()).isEqualTo("Smith");
            assertThat(result.get(3).getCode()).matches(uuidPattern);
            assertThat(result.get(3).getLabel()).isEqualTo("Unknown");
            assertThat(result.get(4).getCode()).matches(uuidPattern);
            assertThat(result.get(4).getLabel()).isEqualTo("Name not known");
            // Ensure all codes are unique
            assertThat(result.stream().map(DynamicStringListElement::getCode).distinct())
                .hasSize(5);
        }

        @Test
        @DisplayName("Should handle null defendant details in list")
        void shouldHandleNullDefendantDetailsInList() {
            // Given
            List<ListValue<DefendantDetails>> allDefendants = List.of(
                ListValue.<DefendantDetails>builder().value(null).build()
            );

            // When
            List<DynamicStringListElement> result = underTest.buildDefendantListItems(allDefendants);

            // Then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getCode()).matches(Pattern.compile(
                "^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$",
                Pattern.CASE_INSENSITIVE));
            assertThat(result.get(0).getLabel()).isEqualTo("Unknown");
        }
    }

    private static Stream<Arguments> singleDefendantScenarios() {
        AddressUK correspondenceAddress = mock(AddressUK.class);

        return Stream.of(
            argumentSet(
                "Name and address not known",

                // Case data DefendantDetails
                DefendantDetails.builder()
                    .nameKnown(VerticalYesNo.NO)
                    .addressKnown(VerticalYesNo.NO)
                    .build(),

                // Expected Defendant
                Defendant.builder()
                    .nameKnown(false)
                    .addressKnown(false)
                    .additionalDefendantsAdded(false)
                    .build()
            ),

            argumentSet(
                "Name known, address not known",

                // Case data DefendantDetails
                DefendantDetails.builder()
                    .nameKnown(VerticalYesNo.YES)
                    .firstName("expected first name")
                    .lastName(("expected last name"))
                    .addressKnown(VerticalYesNo.NO)
                    .build(),

                // Expected Defendant
                Defendant.builder()
                    .nameKnown(true)
                    .firstName("expected first name")
                    .lastName("expected last name")
                    .addressKnown(false)
                    .additionalDefendantsAdded(false)
                    .build()
            ),

            argumentSet(
                "Name not known, address same as possession",

                // Case data DefendantDetails
                DefendantDetails.builder()
                    .nameKnown(VerticalYesNo.NO)
                    .addressKnown(VerticalYesNo.YES)
                    .addressSameAsPossession(VerticalYesNo.YES)
                    .build(),

                // Expected Defendant
                Defendant.builder()
                    .nameKnown(false)
                    .addressKnown(true)
                    .addressSameAsPossession(true)
                    .additionalDefendantsAdded(false)
                    .build()
            ),

            argumentSet(
                "Name known, different correspondence address",

                // Case data DefendantDetails
                DefendantDetails.builder()
                    .nameKnown(VerticalYesNo.YES)
                    .firstName("expected first name")
                    .lastName(("expected last name"))
                    .addressKnown(VerticalYesNo.YES)
                    .addressSameAsPossession(VerticalYesNo.NO)
                    .correspondenceAddress(correspondenceAddress)
                    .build(),

                // Expected Defendant
                Defendant.builder()
                    .nameKnown(true)
                    .firstName("expected first name")
                    .lastName("expected last name")
                    .addressKnown(true)
                    .addressSameAsPossession(false)
                    .correspondenceAddress(correspondenceAddress)
                    .additionalDefendantsAdded(false)
                    .build()
            )
        );
    }
}
