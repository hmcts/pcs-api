package uk.gov.hmcts.reform.pcs.ccd.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.ccd.sdk.type.ListValue;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.Party;

import java.util.ArrayList;
import java.util.List;

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

    @Test
    void shouldFormatCaseNameWhenClaimantIsOrganisationAndSingleDefendantIsKnown() {
        // Given
        Party claimant = organisationClaimant("Treetops Housing");
        Party defendant = defendant("David", "Jackson");

        // When
        String caseName = underTest.formatCaseName(List.of(claimant), List.of(defendant));

        // Then
        assertThat(caseName).isEqualTo("Treetops Housing vs David Jackson");
    }

    @Test
    void shouldFormatCaseNameWhenClaimantIsIndividualAndSingleDefendantIsKnown() {
        // Given
        Party claimant = individualClaimant("Sarah", "Freeman");
        Party defendant = defendant("David", "Jackson");

        // When
        String caseName = underTest.formatCaseName(List.of(claimant), List.of(defendant));

        // Then
        assertThat(caseName).isEqualTo("Sarah Freeman vs David Jackson");
    }

    @Test
    void shouldFormatCaseNameWhenOnlyLastNamesAreAvailable() {
        // Given
        Party claimant = individualClaimant(null, "Freeman");
        Party defendant = defendant(null, "Jackson");

        // When
        String caseName = underTest.formatCaseName(List.of(claimant), List.of(defendant));

        // Then
        assertThat(caseName).isEqualTo("Freeman vs Jackson");
    }

    @Test
    void shouldFormatCaseNameWhenDefendantsAreNull() {
        // Given
        Party claimant = individualClaimant("Sarah", "Freeman");

        // When
        String caseName = underTest.formatCaseName(List.of(claimant), null);

        // Then
        assertThat(caseName).isEqualTo("Sarah Freeman vs Persons unknown");
    }

    @Test
    void shouldFormatCaseNameWhenDefendantsAreEmpty() {
        // Given
        Party claimant = individualClaimant("Sarah", "Freeman");

        // When
        String caseName = underTest.formatCaseName(List.of(claimant), List.of());

        // Then
        assertThat(caseName).isEqualTo("Sarah Freeman vs Persons unknown");
    }

    @Test
    void shouldFormatCaseNameWhenClaimantIsOrganisationAndThereAreTwoDefendants() {
        // Given
        Party claimant = organisationClaimant("Treetops Housing");
        Party defendantOne = defendant("David", "Jackson");
        Party defendantTwo = defendant("Jane", "Smith");

        // When
        String caseName = underTest.formatCaseName(List.of(claimant), List.of(defendantOne, defendantTwo));

        // Then
        assertThat(caseName).isEqualTo("Treetops Housing vs David Jackson and Jane Smith");
    }

    @Test
    void shouldFormatCaseNameWhenClaimantIsIndividualAndThereAreMoreThanTwoDefendants() {
        // Given
        Party claimant = individualClaimant("Sarah", "Freeman");
        Party defendantOne = defendant("David", "Jackson");
        Party defendantTwo = defendant("Jane", "Smith");
        Party defendantThree = defendant("Sam", "Taylor");

        // When
        String caseName = underTest.formatCaseName(
            List.of(claimant),
            List.of(defendantOne, defendantTwo, defendantThree)
        );

        // Then
        assertThat(caseName).isEqualTo("Sarah Freeman vs David Jackson, Jane Smith and Others");
    }

    @Test
    void shouldFormatCaseNameWhenClaimantIsOrganisationAndThereAreMoreThanTwoDefendants() {
        // Given
        Party claimant = organisationClaimant("Treetops Housing");
        Party defendantOne = defendant("David", "Jackson");
        Party defendantTwo = defendant("Jane", "Smith");
        Party defendantThree = defendant("Sam", "Taylor");

        // When
        String caseName = underTest.formatCaseName(
            List.of(claimant),
            List.of(defendantOne, defendantTwo, defendantThree)
        );

        // Then
        assertThat(caseName).isEqualTo("Treetops Housing vs David Jackson, Jane Smith and Others");
    }

    @Test
    void shouldFormatCaseNameWhenDefendantNameIsNotKnown() {
        // Given
        Party claimant = organisationClaimant("Treetops Housing");
        Party defendant = Party.builder()
            .nameKnown(NO)
            .build();

        // When
        String caseName = underTest.formatCaseName(List.of(claimant), List.of(defendant));

        // Then
        assertThat(caseName).isEqualTo("Treetops Housing vs Persons unknown");
    }

    @Test
    void shouldFormatCaseNameWhenIndividualClaimantAndSingleDefendantNameIsNotKnown() {
        // Given
        Party claimant = individualClaimant("Sarah", "Freeman");
        Party defendant = unknownDefendant();

        // When
        String caseName = underTest.formatCaseName(List.of(claimant), List.of(defendant));

        // Then
        assertThat(caseName).isEqualTo("Sarah Freeman vs Persons unknown");
    }

    @Test
    void shouldFormatCaseNameWhenIndividualClaimantAndTwoDefendantNamesAreNotKnown() {
        // Given
        Party claimant = individualClaimant("Sarah", "Freeman");
        Party defendantOne = unknownDefendant();
        Party defendantTwo = unknownDefendant();

        // When
        String caseName = underTest.formatCaseName(List.of(claimant), List.of(defendantOne, defendantTwo));

        // Then
        assertThat(caseName).isEqualTo("Sarah Freeman vs Persons unknown and Persons unknown");
    }

    @Test
    void shouldFormatCaseNameWhenOrganisationClaimantAndTwoDefendantNamesAreNotKnown() {
        // Given
        Party claimant = organisationClaimant("Treetops Housing");
        Party defendantOne = unknownDefendant();
        Party defendantTwo = unknownDefendant();

        // When
        String caseName = underTest.formatCaseName(List.of(claimant), List.of(defendantOne, defendantTwo));

        // Then
        assertThat(caseName).isEqualTo("Treetops Housing vs Persons unknown and Persons unknown");
    }

    @Test
    void shouldFormatCaseNameWhenDefendantEntryIsNull() {
        // Given
        Party claimant = individualClaimant("Sarah", "Freeman");
        List<Party> defendants = new ArrayList<>();
        defendants.add(null);

        // When
        String caseName = underTest.formatCaseName(List.of(claimant), defendants);

        // Then
        assertThat(caseName).isEqualTo("Sarah Freeman vs Persons unknown");
    }

    @Test
    void shouldFormatCaseNameWhenIndividualClaimantAndMoreThanTwoDefendantNamesAreNotKnown() {
        // Given
        Party claimant = individualClaimant("Sarah", "Freeman");

        // When
        String caseName = underTest.formatCaseName(
            List.of(claimant),
            List.of(unknownDefendant(), unknownDefendant(), unknownDefendant())
        );

        // Then
        assertThat(caseName).isEqualTo("Sarah Freeman vs Persons unknown, Persons unknown and Others");
    }

    @Test
    void shouldFormatCaseNameWhenOrganisationClaimantAndMoreThanTwoDefendantNamesAreNotKnown() {
        // Given
        Party claimant = organisationClaimant("Treetops Housing");

        // When
        String caseName = underTest.formatCaseName(
            List.of(claimant),
            List.of(unknownDefendant(), unknownDefendant(), unknownDefendant())
        );

        // Then
        assertThat(caseName).isEqualTo("Treetops Housing vs Persons unknown, Persons unknown and Others");
    }

    @Test
    void shouldFormatCaseNameWhenIndividualClaimantAndSecondDefendantNameIsNotKnown() {
        // Given
        Party claimant = individualClaimant("Sarah", "Freeman");

        // When
        String caseName = underTest.formatCaseName(
            List.of(claimant),
            List.of(defendant("David", "Jackson"), unknownDefendant())
        );

        // Then
        assertThat(caseName).isEqualTo("Sarah Freeman vs David Jackson and Persons unknown");
    }

    @Test
    void shouldFormatCaseNameWhenOrganisationClaimantAndSecondDefendantNameIsNotKnown() {
        // Given
        Party claimant = organisationClaimant("Treetops Housing");

        // When
        String caseName = underTest.formatCaseName(
            List.of(claimant),
            List.of(defendant("David", "Jackson"), unknownDefendant())
        );

        // Then
        assertThat(caseName).isEqualTo("Treetops Housing vs David Jackson and Persons unknown");
    }

    @Test
    void shouldFormatCaseNameWhenIndividualClaimantAndFirstDefendantNameIsNotKnown() {
        // Given
        Party claimant = individualClaimant("Sarah", "Freeman");

        // When
        String caseName = underTest.formatCaseName(
            List.of(claimant),
            List.of(unknownDefendant(), defendant("David", "Jackson"))
        );

        // Then
        assertThat(caseName).isEqualTo("Sarah Freeman vs Persons unknown and David Jackson");
    }

    @Test
    void shouldFormatCaseNameWhenOrganisationClaimantAndFirstDefendantNameIsNotKnown() {
        // Given
        Party claimant = organisationClaimant("Treetops Housing");

        // When
        String caseName = underTest.formatCaseName(
            List.of(claimant),
            List.of(unknownDefendant(), defendant("David", "Jackson"))
        );

        // Then
        assertThat(caseName).isEqualTo("Treetops Housing vs Persons unknown and David Jackson");
    }

    @Test
    void shouldFormatCaseNameWhenIndividualClaimantAndMoreThanTwoDefendantsIncludeUnknownName() {
        // Given
        Party claimant = individualClaimant("Sarah", "Freeman");

        // When
        String caseName = underTest.formatCaseName(
            List.of(claimant),
            List.of(defendant("David", "Jackson"), unknownDefendant(), defendant("Sam", "Taylor"))
        );

        // Then
        assertThat(caseName).isEqualTo("Sarah Freeman vs David Jackson, Persons unknown and Others");
    }

    @Test
    void shouldFormatCaseNameWhenOrganisationClaimantAndMoreThanTwoDefendantsIncludeUnknownName() {
        // Given
        Party claimant = organisationClaimant("Treetops Housing");

        // When
        String caseName = underTest.formatCaseName(
            List.of(claimant),
            List.of(defendant("David", "Jackson"), unknownDefendant(), defendant("Sam", "Taylor"))
        );

        // Then
        assertThat(caseName).isEqualTo("Treetops Housing vs David Jackson, Persons unknown and Others");
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
