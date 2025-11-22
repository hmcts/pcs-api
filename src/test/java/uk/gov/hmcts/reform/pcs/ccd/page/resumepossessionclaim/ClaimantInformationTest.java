package uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import uk.gov.hmcts.reform.pcs.ccd.domain.ClaimantCircumstances;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.page.BasePageTest;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class ClaimantInformationTest extends BasePageTest {

    @BeforeEach
    void setUp() {
        setPageUnderTest(new ClaimantInformation());
    }

    @Test
    void shouldHandleNullOrganisationNameAndNullClaimantName() {
        // Given
        PCSCase caseData = PCSCase.builder()
            .organisationName(null)
            .claimantName(null)
            .claimantCircumstances(ClaimantCircumstances.builder().build())
            .build();

        // When
        callMidEventHandler(caseData);

        // Then
        assertThat(caseData.getClaimantCircumstances().getClaimantNamePossessiveForm()).isNull();
    }

    @Test
    void shouldFallbackToClaimantNameWhenOrganisationNameIsNull() {
        // Given
        PCSCase caseData = PCSCase.builder()
            .organisationName(null)
            .claimantName("Claimant Name")
            .claimantCircumstances(ClaimantCircumstances.builder().build())
            .build();

        // When
        callMidEventHandler(caseData);

        // Then
        assertThat(caseData.getClaimantCircumstances().getClaimantNamePossessiveForm())
            .isEqualTo("Claimant Name's");
    }

    @Test
    void shouldFallbackToClaimantNameWhenOrganisationNameIsEmpty() {
        // Given
        PCSCase caseData = PCSCase.builder()
            .organisationName("")
            .claimantName("Claimant Name")
            .claimantCircumstances(ClaimantCircumstances.builder().build())
            .build();

        // When
        callMidEventHandler(caseData);

        // Then
        assertThat(caseData.getClaimantCircumstances().getClaimantNamePossessiveForm())
            .isEqualTo("Claimant Name's");
    }

    @ParameterizedTest
    @MethodSource("claimantNamesEndingWithSTestData")
    @DisplayName("Should append apostrophe to organisation names ending with 's' or 'S': {0}")
    void shouldAppendApostropheToOrganisationNamesEndingWithS(@SuppressWarnings("unused") String testDescription,
                                                          String organisationName,
                                                          String overriddenOrganisationName,
                                                          String expectedDisplayedName) {
        // Given
        PCSCase caseData = PCSCase.builder().organisationName(organisationName)
            .overriddenClaimantName(overriddenOrganisationName)
            .claimantCircumstances(ClaimantCircumstances.builder().build()).build();

        // When
        callMidEventHandler(caseData);

        // Then
        assertThat(caseData.getClaimantCircumstances().getClaimantNamePossessiveForm())
            .isEqualTo(expectedDisplayedName);
    }

    @ParameterizedTest
    @MethodSource("claimantNameTestData")
    @DisplayName("{0}")
    void shouldHandleDisplayedOrganisationName(@SuppressWarnings("unused") String testDescription,
                                           String organisationName,
                                           String overriddenOrganisationName,
                                           String expectedDisplayedClaimantName) {
        // Given
        PCSCase caseData = PCSCase.builder().organisationName(organisationName)
            .overriddenClaimantName(overriddenOrganisationName)
            .claimantCircumstances(ClaimantCircumstances.builder().build()).build();

        // When
        callMidEventHandler(caseData);

        // Then
        assertThat(caseData.getClaimantCircumstances().getClaimantNamePossessiveForm())
            .isEqualTo(expectedDisplayedClaimantName);
    }

    private static Stream<Arguments> claimantNameTestData() {
        return Stream.of(
            testData("should set displayed organisation name from overridden claimant name",
                     "Organisation Name", "Overridden Organisation Name"),
            testData("should set displayed organisation name from organisation name when no override",
                     "Organisation Name", null),
            testData("should handle name with leading and trailing spaces",
                     " Organisation Name ", null),
            testData("should handle empty overridden claimant name",
                     "Organisation Name", "")
        );
    }

    private static Stream<Arguments> claimantNamesEndingWithSTestData() {
        return Stream.of(
            testCase("should handle organisation name with special characters",
                     "O'Brien & Associates", null),
            testCase("should handle single character S", "S", null),
            testCase("should handle uppercase S", "ORGANISATION NAMES", null),
            testCase("should set displayed organisation name from organisation name ends with S",
                     "Organisation Names", null),
            testCase("should set displayed organisation name from overridden claimant name ends with S",
                     "Organisation Name", "Overridden Organisation Names")
        );
    }

    private static Arguments testCase(String description, String organisationName, String overriddenName) {
        String nameToUse = overriddenName != null ? overriddenName : organisationName;
        return Arguments.of(description, organisationName, overriddenName, nameToUse + "'");
    }

    private static Arguments testData(String description, String organisationName, String overriddenName) {
        String nameToUse = (overriddenName != null && !overriddenName.isEmpty()) ? overriddenName : organisationName;
        String expectedResult = nameToUse == null ? null : nameToUse + "'s";
        return Arguments.of(description, organisationName, overriddenName, expectedResult);
    }

}
