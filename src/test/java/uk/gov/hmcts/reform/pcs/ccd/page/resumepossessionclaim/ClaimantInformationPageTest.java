package uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import uk.gov.hmcts.reform.pcs.ccd.domain.ClaimantCircumstances;
import uk.gov.hmcts.reform.pcs.ccd.domain.ClaimantInformation;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.page.BasePageTest;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class ClaimantInformationPageTest extends BasePageTest {

    @BeforeEach
    void setUp() {
        setPageUnderTest(new ClaimantInformationPage());
    }

    @Test
    void shouldHandleNullOrganisationNameAndNullClaimantName() {
        // Given
        PCSCase caseData = PCSCase.builder()
            .claimantInformation(
                ClaimantInformation.builder().build()
            )
            .claimantCircumstances(ClaimantCircumstances.builder().build())
            .build();

        // When
        callMidEventHandler(caseData);

        // Then
        assertThat(caseData.getClaimantCircumstances().getClaimantNamePossessiveForm()).isNull();
    }

    @ParameterizedTest
    @MethodSource("claimantNamesEndingWithSTestData")
    @DisplayName("Should append apostrophe to organisation names ending with ’s’ or ’S’: {0}")
    void shouldAppendApostropheToOrganisationNamesEndingWithS(@SuppressWarnings("unused") String testDescription,
                                                          String claimantName,
                                                          String overriddenClaimantName,
                                                          String expectedDisplayedName) {
        // Given
        PCSCase caseData = PCSCase.builder()
            .claimantInformation(
                ClaimantInformation.builder()
                    .claimantName(claimantName)
                    .overriddenClaimantName(overriddenClaimantName)
                    .build()
            )
            .claimantCircumstances(ClaimantCircumstances.builder().build())
            .build();

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
                                           String claimantName,
                                           String overriddenClaimantName,
                                           String expectedDisplayedClaimantName) {
        // Given
        PCSCase caseData = PCSCase.builder()
            .claimantInformation(
                ClaimantInformation.builder()
                    .claimantName(claimantName)
                    .overriddenClaimantName(overriddenClaimantName)
                    .build()
            )
            .claimantCircumstances(ClaimantCircumstances.builder().build())
            .build();

        // When
        callMidEventHandler(caseData);

        // Then
        assertThat(caseData.getClaimantCircumstances().getClaimantNamePossessiveForm())
            .isEqualTo(expectedDisplayedClaimantName);
    }

    @ParameterizedTest
    @MethodSource("namesWithExistingApostropheTestData")
    @DisplayName("Should handle names that already end with apostrophe: {0}")
    void shouldHandleNamesWithExistingApostrophe(@SuppressWarnings("unused") String testDescription,
                                                String claimantName,
                                                String overriddenClaimantName,
                                                String expectedDisplayedName) {
        // Given
        PCSCase caseData = PCSCase.builder()
            .claimantInformation(
                ClaimantInformation.builder()
                    .claimantName(claimantName)
                    .overriddenClaimantName(overriddenClaimantName)
                    .build()
            )
            .claimantCircumstances(ClaimantCircumstances.builder().build())
            .build();

        // When
        callMidEventHandler(caseData);

        // Then
        assertThat(caseData.getClaimantCircumstances().getClaimantNamePossessiveForm())
            .isEqualTo(expectedDisplayedName);
    }

    @ParameterizedTest
    @MethodSource("realWorldHousingSocietyNamesTestData")
    @DisplayName("Should handle real-world housing society and organization names: {0}")
    void shouldHandleRealWorldHousingSocietyNames(@SuppressWarnings("unused") String testDescription,
                                                 String claimantName,
                                                 String overriddenClaimantName,
                                                 String expectedDisplayedName) {
        // Given
        PCSCase caseData = PCSCase.builder()
            .claimantInformation(
                ClaimantInformation.builder()
                    .claimantName(claimantName)
                    .overriddenClaimantName(overriddenClaimantName)
                    .build()
            )
            .claimantCircumstances(ClaimantCircumstances.builder().build())
            .build();

        // When
        callMidEventHandler(caseData);

        // Then
        assertThat(caseData.getClaimantCircumstances().getClaimantNamePossessiveForm())
            .isEqualTo(expectedDisplayedName);
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
                     "O’Brien & Associates", null),
            testCase("should handle single character S", "S", null),
            testCase("should handle uppercase S", "ORGANISATION NAMES", null),
            testCase("should set displayed organisation name from organisation name ends with S",
                     "Organisation Names", null),
            testCase("should set displayed organisation name from overridden claimant name ends with S",
                     "Organisation Name", "Overridden Organisation Names")
        );
    }

    private static Arguments testCase(String description, String claimantName, String overriddenName) {
        String nameToUse = overriddenName != null ? overriddenName : claimantName;
        return Arguments.of(description, claimantName, overriddenName, nameToUse.trim() + "’");
    }

    private static Arguments testData(String description, String claimantName, String overriddenName) {
        String nameToUse = (overriddenName != null && !overriddenName.isEmpty()) ? overriddenName : claimantName;
        String expectedResult = nameToUse == null ? null : nameToUse.trim() + "’s";
        return Arguments.of(description, claimantName, overriddenName, expectedResult);
    }

    private static Stream<Arguments> namesWithExistingApostropheTestData() {
        return Stream.of(
            Arguments.of("should handle name ending with apostrophe and S (HDPI’S)",
                        "HDPI’S", null, "HDPI’S"),
            Arguments.of("should handle name ending with ’s (James’s)",
                        "James’s", null, "James’s"),
            Arguments.of("should handle name ending with ’S (JAMES’S)",
                        "JAMES’S", null, "JAMES’S"),
            Arguments.of("should handle name ending with just apostrophe",
                        "Name’", null, "Name’"),
            Arguments.of("should handle name with trailing space",
                        "Name ", null, "Name’s"),
            Arguments.of("should handle empty string",
                        "", null, ""),
            Arguments.of("should handle overridden name ending with apostrophe",
                        "Organisation Name", "HDPI’S", "HDPI’S"),
            Arguments.of("should handle name ending with s but not possessive",
                        "Class", null, "Class’")
        );
    }

    private static Stream<Arguments> realWorldHousingSocietyNamesTestData() {
        return Stream.of(
            Arguments.of("should handle St. James’s Housing Association (apostrophe in middle, ends with ’n’)",
                        "St. James’s Housing Association", null, "St. James’s Housing Association’s"),
            Arguments.of("should handle St. Mary’s Housing Trust (apostrophe in middle, ends with ’t’)",
                        "St. Mary’s Housing Trust", null, "St. Mary’s Housing Trust’s"),
            Arguments.of("should handle Children’s Services (already possessive, ends with ’s’)",
                        "Children’s Services", null, "Children’s Services’"),
            Arguments.of("should handle Women’s Aid (already possessive, ends with ’d’)",
                        "Women’s Aid", null, "Women’s Aid’s"),
            Arguments.of("should handle Men’s Housing (already possessive, ends with ’g’)",
                        "Men’s Housing", null, "Men’s Housing’s"),
            Arguments.of("should handle People’s Housing Association (already possessive, ends with ’n’)",
                        "People’s Housing Association", null, "People’s Housing Association’s"),
            Arguments.of("should handle name with leading space",
                        " Name", null, "Name’s"),
            Arguments.of("should handle name with leading and trailing spaces",
                        " Housing Trust ", null, "Housing Trust’s"),
            Arguments.of("should handle name that is just whitespace",
                        "   ", null, ""),
            Arguments.of("should handle O’Brien & Associates (apostrophe in middle, ends with ’s’)",
                        "O’Brien & Associates", null, "O’Brien & Associates’"),
            Arguments.of("should handle name with numbers ending in s (2024 Services)",
                        "2024 Services", null, "2024 Services’"),
            Arguments.of("should handle single character ’s’",
                        "s", null, "s’"),
            Arguments.of("should handle single character ’S’",
                        "S", null, "S’"),
            Arguments.of("should handle name ending with multiple s characters",
                        "Housing Services", null, "Housing Services’"),
            Arguments.of("should handle name with unicode ending in s",
                        "Housing Servicés", null, "Housing Servicés’"),
            Arguments.of("should handle name ending with ’s (already fully possessive)",
                        "James’s", null, "James’s"),
            Arguments.of("should handle name ending with ’S (uppercase possessive)",
                        "JAMES’S", null, "JAMES’S"),
            Arguments.of("should handle name with tab character",
                        "Name\t", null, "Name’s"),
            Arguments.of("should handle name with newline character",
                        "Name\n", null, "Name’s")
        );
    }

}
