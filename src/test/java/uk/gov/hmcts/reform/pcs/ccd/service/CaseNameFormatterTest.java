package uk.gov.hmcts.reform.pcs.ccd.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import uk.gov.hmcts.ccd.sdk.type.ListValue;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.Party;

import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo.NO;
import static uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo.YES;

class CaseNameFormatterTest {

    private CaseNameFormatter underTest;

    @BeforeEach
    void setUp() {
        underTest = new CaseNameFormatter();
    }

    @Test
    void shouldFormatCaseNameFromPcsCase() {
        // Given
        PCSCase pcsCase = PCSCase.builder()
            .allClaimants(List.of(listValue(individualClaimant("Sarah", "Freeman"))))
            .allDefendants(List.of(listValue(defendant("David", "Jackson"))))
            .build();

        // When
        String caseName = underTest.formatCaseName(pcsCase);

        // Then
        assertThat(caseName).isEqualTo("Sarah Freeman vs David Jackson");
    }

    @ParameterizedTest
    @MethodSource("claimantAndDefendantScenarios")
    void shouldFormatCaseName(List<Party> allClaimants, List<Party> allDefendants, String output) {
        // When
        String caseName = underTest.formatCaseName(allClaimants, allDefendants);

        // Then
        assertThat(caseName).isEqualTo(output);
    }

    private static Stream<Arguments> claimantAndDefendantScenarios() {
        return Stream.of(
            Arguments.argumentSet(
                "Claimant is organisation and single defendant is known",
                List.of(organisationClaimant("Treetops Housing")),
                List.of(defendant("David", "Jackson")),
                "Treetops Housing vs David Jackson"
            ),
            Arguments.argumentSet(
                "Claimant is individual and single defendant is known",
                List.of(individualClaimant("Sarah", "Freeman")),
                List.of(defendant("David", "Jackson")),
                "Sarah Freeman vs David Jackson"
            ),
            Arguments.argumentSet(
                "Only last names are available",
                List.of(individualClaimant(null, "Freeman")),
                List.of(defendant(null, "Jackson")),
                "Freeman vs Jackson"
            ),
            Arguments.argumentSet(
                "Defendants are null",
                List.of(individualClaimant("Sarah", "Freeman")),
                null,
                "Sarah Freeman vs Persons unknown"
            ),
            Arguments.argumentSet(
                "Defendants are empty",
                List.of(individualClaimant("Sarah", "Freeman")),
                List.of(),
                "Sarah Freeman vs Persons unknown"
            ),
            Arguments.argumentSet(
                "Claimant is organisation and two defendants",
                List.of(organisationClaimant("Treetops Housing")),
                List.of(defendant("David", "Jackson"), defendant("Jane", "Smith")),
                "Treetops Housing vs David Jackson and Jane Smith"
            ),
            Arguments.argumentSet(
                "Claimant is individual and more than two defendants",
                List.of(individualClaimant("Sarah", "Freeman")),
                List.of(defendant("David", "Jackson"), defendant("Jane", "Smith"), defendant("Sam", "Taylor")),
                "Sarah Freeman vs David Jackson, Jane Smith and Others"
            ),
            Arguments.argumentSet(
                "Claimant is organisation and more than two defendants",
                List.of(organisationClaimant("Treetops Housing")),
                List.of(defendant("David", "Jackson"), defendant("Jane", "Smith"), defendant("Sam", "Taylor")),
                "Treetops Housing vs David Jackson, Jane Smith and Others"
            ),
            Arguments.argumentSet(
                "Claimant is organisation and defendant name not known",
                List.of(organisationClaimant("Treetops Housing")),
                List.of(unknownDefendant()),
                "Treetops Housing vs Persons unknown"
            ),
            Arguments.argumentSet(
                "Claimant is individual and defendant name not known",
                List.of(individualClaimant("Sarah", "Freeman")),
                List.of(unknownDefendant()),
                "Sarah Freeman vs Persons unknown"
            ),
            Arguments.argumentSet(
                "Claimant is individual and two defendant names not known",
                List.of(individualClaimant("Sarah", "Freeman")),
                List.of(unknownDefendant(), unknownDefendant()),
                "Sarah Freeman vs Persons unknown and Persons unknown"
            ),
            Arguments.argumentSet(
                "Claimant is organisation and two defendant names not known",
                List.of(organisationClaimant("Treetops Housing")),
                List.of(unknownDefendant(), unknownDefendant()),
                "Treetops Housing vs Persons unknown and Persons unknown"
            ),
            Arguments.argumentSet(
                "Claimant is individual and more than two defendant names not known",
                List.of(individualClaimant("Sarah", "Freeman")),
                List.of(unknownDefendant(), unknownDefendant(), unknownDefendant()),
                "Sarah Freeman vs Persons unknown, Persons unknown and Others"
            ),
            Arguments.argumentSet(
                "Claimant is individual and second defendant is not known",
                List.of(individualClaimant("Sarah", "Freeman")),
                List.of(defendant("David", "Jackson"), unknownDefendant()),
                "Sarah Freeman vs David Jackson and Persons unknown"
            ),
            Arguments.argumentSet(
                "Claimant is organisation and second defendant is not known",
                List.of(organisationClaimant("Treetops Housing")),
                List.of(defendant("David", "Jackson"), unknownDefendant()),
                "Treetops Housing vs David Jackson and Persons unknown"
            ),
            Arguments.argumentSet(
                "Claimant is individual and first defendant is not known",
                List.of(individualClaimant("Sarah", "Freeman")),
                List.of(unknownDefendant(), defendant("David", "Jackson")),
                "Sarah Freeman vs Persons unknown and David Jackson"
            ),
            Arguments.argumentSet(
                "Claimant is organisation and first defendant is not known",
                List.of(organisationClaimant("Treetops Housing")),
                List.of(unknownDefendant(), defendant("David", "Jackson")),
                "Treetops Housing vs Persons unknown and David Jackson"
            ),
            Arguments.argumentSet(
                "Claimant is individual and more than two defendants and second defendant is not known",
                List.of(individualClaimant("Sarah", "Freeman")),
                List.of(defendant("David", "Jackson"), unknownDefendant(), defendant("Sam", "Taylor")),
                "Sarah Freeman vs David Jackson, Persons unknown and Others"
            ),
            Arguments.argumentSet(
                "Claimant is organisation and more than two defendants and first defendant is not known",
                List.of(organisationClaimant("Treetops Housing")),
                List.of(defendant("David", "Jackson"), unknownDefendant(), defendant("Sam", "Taylor")),
                "Treetops Housing vs David Jackson, Persons unknown and Others"
            ),
            Arguments.argumentSet(
                "All claimants is empty",
                List.of(),
                List.of(defendant("David", "Jackson")),
                "null vs David Jackson"
            )
        );
    }

    private static Party organisationClaimant(String organisationName) {
        return Party.builder()
            .orgName(organisationName)
            .build();
    }

    private static Party individualClaimant(String firstName, String lastName) {
        return Party.builder()
            .firstName(firstName)
            .lastName(lastName)
            .build();
    }

    private static Party defendant(String firstName, String lastName) {
        return Party.builder()
            .nameKnown(YES)
            .firstName(firstName)
            .lastName(lastName)
            .build();
    }

    private static Party unknownDefendant() {
        return Party.builder()
            .nameKnown(NO)
            .build();
    }

    private static <T> ListValue<T> listValue(T value) {
        return ListValue.<T>builder()
            .value(value)
            .build();
    }
}
