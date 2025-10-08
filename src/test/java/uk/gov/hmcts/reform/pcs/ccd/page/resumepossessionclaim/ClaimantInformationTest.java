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
    void shouldHandleNullClaimantName() {
        // Given
        PCSCase caseData = PCSCase.builder()
            .claimantName(null)
            .claimantCircumstances(ClaimantCircumstances.builder().build())
            .build();

        // When
        callMidEventHandler(caseData);

        // Then
        assertThat(caseData.getClaimantCircumstances().getClaimantNamePossessiveForm()).isNull();
    }

    @ParameterizedTest
    @MethodSource("claimantNamesEndingWithSTestData")
    @DisplayName("Should append apostrophe to claimant names ending with 's' or 'S': {0}")
    void shouldAppendApostropheToClaimantNamesEndingWithS(@SuppressWarnings("unused") String testDescription,
                                                          String claimantName,
                                                          String overriddenClaimantName,
                                                          String expectedDisplayedName) {
        // Given
        PCSCase caseData = PCSCase.builder().claimantName(claimantName)
            .overriddenClaimantName(overriddenClaimantName)
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
    void shouldHandleDisplayedClaimantName(@SuppressWarnings("unused") String testDescription,
                                           String claimantName,
                                           String overriddenClaimantName,
                                           String expectedDisplayedClaimantName) {
        // Given
        PCSCase caseData = PCSCase.builder().claimantName(claimantName)
            .overriddenClaimantName(overriddenClaimantName)
            .claimantCircumstances(ClaimantCircumstances.builder().build()).build();

        // When
        callMidEventHandler(caseData);

        // Then
        assertThat(caseData.getClaimantCircumstances().getClaimantNamePossessiveForm())
            .isEqualTo(expectedDisplayedClaimantName);
    }

    private static Stream<Arguments> claimantNameTestData() {
        return Stream.of(
            testData("should set displayed claimant name from overridden claimant name",
                     "Claimant Name", "Overridden Claimant Name"),
            testData("should set displayed claimant name from claimant name when no override",
                     "Claimant Name", null),
            testData("should handle name with leading and trailing spaces",
                     " Claimant Name ", null),
            testData("should handle empty overridden claimant name",
                     "Claimant Name", "")
        );
    }

    private static Stream<Arguments> claimantNamesEndingWithSTestData() {
        return Stream.of(
            testCase("should handle claimant name with special characters",
                     "O'Brien & Associates", null),
            testCase("should handle single character S", "S", null),
            testCase("should handle uppercase S", "CLAIMANT NAMES", null),
            testCase("should set displayed claimant name from claimant name ends with S",
                     "Claimant Names", null),
            testCase("should set displayed claimant name from overridden claimant name ends with S",
                     "Claimant Name", "Overridden Claimant Names")
        );
    }

    private static Arguments testCase(String description, String claimantName, String overriddenName) {
        String nameToUse = overriddenName != null ? overriddenName : claimantName;
        return Arguments.of(description, claimantName, overriddenName, nameToUse + "'");
    }

    private static Arguments testData(String description, String claimantName, String overriddenName) {
        String nameToUse = (overriddenName != null && !overriddenName.isEmpty()) ? overriddenName : claimantName;
        String expectedResult = nameToUse == null ? null : nameToUse + "'s";
        return Arguments.of(description, claimantName, overriddenName, expectedResult);
    }

}
