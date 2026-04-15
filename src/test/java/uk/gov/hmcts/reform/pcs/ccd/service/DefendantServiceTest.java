package uk.gov.hmcts.reform.pcs.ccd.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.ccd.sdk.type.ListValue;
import uk.gov.hmcts.reform.pcs.ccd.domain.Party;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.type.DynamicStringListElement;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.ParameterizedTest.ARGUMENT_SET_NAME_PLACEHOLDER;
import static org.junit.jupiter.params.provider.Arguments.argumentSet;

@ExtendWith(MockitoExtension.class)
class DefendantServiceTest {

    private DefendantService underTest;

    @BeforeEach
    void setUp() {
        underTest = new DefendantService();
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
            Party details = Party.builder()
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
            Party details, String expectedDisplayName) {
            // When
            String result = underTest.buildDefendantDisplayName(details);

            // Then
            assertThat(result).isEqualTo(expectedDisplayName);
        }

        @Test
        @DisplayName("Should trim whitespace from final result")
        void shouldTrimWhitespaceFromFinalResult() {
            // Given
            Party details = Party.builder()
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
                    Party.builder()
                        .nameKnown(VerticalYesNo.YES)
                        .firstName("John")
                        .lastName("Doe")
                        .build(),
                    "John Doe"
                ),
                argumentSet(
                    "First name only when last name is null",
                    Party.builder()
                        .nameKnown(VerticalYesNo.YES)
                        .firstName("John")
                        .lastName(null)
                        .build(),
                    "John"
                ),
                argumentSet(
                    "Last name only when first name is null",
                    Party.builder()
                        .nameKnown(VerticalYesNo.YES)
                        .firstName(null)
                        .lastName("Doe")
                        .build(),
                    "Doe"
                ),
                argumentSet(
                    "Unknown when both names are empty strings",
                    Party.builder()
                        .nameKnown(VerticalYesNo.YES)
                        .firstName("")
                        .lastName("")
                        .build(),
                    "Unknown"
                ),
                argumentSet(
                    "Unknown when both names are null",
                    Party.builder()
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
            String defendant1Id = UUID.randomUUID().toString();

            Party defendantDetails = Party.builder()
                .nameKnown(VerticalYesNo.YES)
                .firstName("John")
                .lastName("Doe")
                .build();

            List<ListValue<Party>> allDefendants = List.of(
                ListValue.<Party>builder().id(defendant1Id).value(defendantDetails).build()
            );

            // When
            List<DynamicStringListElement> result = underTest.buildDefendantListItems(allDefendants);

            // Then
            assertThat(result).hasSize(1);
            assertThat(result.getFirst().getCode()).isEqualTo(defendant1Id);
            assertThat(result.getFirst().getLabel()).isEqualTo("John Doe");
        }

        @Test
        @DisplayName("Should build list with multiple defendants")
        void shouldBuildListWithMultipleDefendants() {
            // Given
            String defendant1Id = UUID.randomUUID().toString();
            String defendant2Id = UUID.randomUUID().toString();
            String defendant3Id = UUID.randomUUID().toString();

            Party defendant1 = Party.builder()
                .nameKnown(VerticalYesNo.YES)
                .firstName("John")
                .lastName("Doe")
                .build();

            Party defendant2 = Party.builder()
                .nameKnown(VerticalYesNo.YES)
                .firstName("Jane")
                .lastName("Smith")
                .build();

            Party defendant3 = Party.builder()
                .nameKnown(VerticalYesNo.NO)
                .build();

            List<ListValue<Party>> allDefendants = List.of(
                ListValue.<Party>builder().id(defendant1Id).value(defendant1).build(),
                ListValue.<Party>builder().id(defendant2Id).value(defendant2).build(),
                ListValue.<Party>builder().id(defendant3Id).value(defendant3).build()
            );

            // When
            List<DynamicStringListElement> result = underTest.buildDefendantListItems(allDefendants);

            // Then
            assertThat(result).hasSize(3);
            assertThat(result.get(0).getCode()).isEqualTo(defendant1Id);
            assertThat(result.get(0).getLabel()).isEqualTo("John Doe");
            assertThat(result.get(1).getCode()).isEqualTo(defendant2Id);
            assertThat(result.get(1).getLabel()).isEqualTo("Jane Smith");
            assertThat(result.get(2).getCode()).isEqualTo(defendant3Id);
            assertThat(result.get(2).getLabel()).isEqualTo("Name not known");
        }

        @Test
        @DisplayName("Should handle mixed scenarios with various name combinations")
        void shouldHandleMixedScenariosWithVariousNameCombinations() {
            // Given
            String defendant1Id = UUID.randomUUID().toString();
            String defendant2Id = UUID.randomUUID().toString();
            String defendant3Id = UUID.randomUUID().toString();
            String defendant4Id = UUID.randomUUID().toString();
            String defendant5Id = UUID.randomUUID().toString();

            Party defendant1 = Party.builder()
                .nameKnown(VerticalYesNo.YES)
                .firstName("John")
                .lastName("Doe")
                .build();

            Party defendant2 = Party.builder()
                .nameKnown(VerticalYesNo.YES)
                .firstName("Jane")
                .lastName(null)
                .build();

            Party defendant3 = Party.builder()
                .nameKnown(VerticalYesNo.YES)
                .firstName(null)
                .lastName("Smith")
                .build();

            Party defendant4 = Party.builder()
                .nameKnown(VerticalYesNo.YES)
                .firstName("")
                .lastName("")
                .build();

            Party defendant5 = Party.builder()
                .nameKnown(VerticalYesNo.NO)
                .build();

            List<ListValue<Party>> allDefendants = List.of(
                ListValue.<Party>builder().id(defendant1Id).value(defendant1).build(),
                ListValue.<Party>builder().id(defendant2Id).value(defendant2).build(),
                ListValue.<Party>builder().id(defendant3Id).value(defendant3).build(),
                ListValue.<Party>builder().id(defendant4Id).value(defendant4).build(),
                ListValue.<Party>builder().id(defendant5Id).value(defendant5).build()
            );

            // When
            List<DynamicStringListElement> result = underTest.buildDefendantListItems(allDefendants);

            // Then
            assertThat(result).hasSize(5);
            assertThat(result.get(0).getCode()).isEqualTo(defendant1Id);
            assertThat(result.get(0).getLabel()).isEqualTo("John Doe");
            assertThat(result.get(1).getCode()).isEqualTo(defendant2Id);
            assertThat(result.get(1).getLabel()).isEqualTo("Jane");
            assertThat(result.get(2).getCode()).isEqualTo(defendant3Id);
            assertThat(result.get(2).getLabel()).isEqualTo("Smith");
            assertThat(result.get(3).getCode()).isEqualTo(defendant4Id);
            assertThat(result.get(3).getLabel()).isEqualTo("Unknown");
            assertThat(result.get(4).getCode()).isEqualTo(defendant5Id);
            assertThat(result.get(4).getLabel()).isEqualTo("Name not known");
        }

        @Test
        @DisplayName("Should handle null defendant details in list")
        void shouldHandleNullDefendantDetailsInList() {
            // Given
            String defendant1Id = UUID.randomUUID().toString();

            List<ListValue<Party>> allDefendants = List.of(
                ListValue.<Party>builder().id(defendant1Id).value(null).build()
            );

            // When
            List<DynamicStringListElement> result = underTest.buildDefendantListItems(allDefendants);

            // Then
            assertThat(result).hasSize(1);
            assertThat(result.getFirst().getCode()).isEqualTo(defendant1Id);
            assertThat(result.getFirst().getLabel()).isEqualTo("Unknown");
        }
    }

}
