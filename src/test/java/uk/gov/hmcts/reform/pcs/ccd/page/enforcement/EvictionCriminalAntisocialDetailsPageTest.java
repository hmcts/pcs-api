package uk.gov.hmcts.reform.pcs.ccd.page.enforcement;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import uk.gov.hmcts.ccd.sdk.api.callback.AboutToStartOrSubmitResponse;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.RiskCategory;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.page.BasePageTest;

import java.util.Set;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;

class EvictionCriminalAntisocialDetailsPageTest extends BasePageTest {

    @BeforeEach
    void setUp() {
        setPageUnderTest(new EvictionCriminalAntisocialDetailsPage());
    }

    @Test
    void shouldRequireText() {
        // Given
        PCSCase caseData = PCSCase.builder()
            .enforcementRiskCategories(Set.of(RiskCategory.CRIMINAL_OR_ANTISOCIAL))
            .enforcementCriminalDetails(null)
            .build();

        // When
        AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData);

        // Then
        assertThat(response.getErrors()).containsExactly("Enter details");
    }

    @Test
    void shouldRequireTextWhenBlank() {
        // Given
        PCSCase caseData = PCSCase.builder()
            .enforcementRiskCategories(Set.of(RiskCategory.CRIMINAL_OR_ANTISOCIAL))
            .enforcementCriminalDetails("   ")
            .build();

        // When
        AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData);

        // Then
        assertThat(response.getErrors()).containsExactly("Enter details");
    }

    @Test
    void shouldRequireTextWhenEmpty() {
        // Given
        PCSCase caseData = PCSCase.builder()
            .enforcementRiskCategories(Set.of(RiskCategory.CRIMINAL_OR_ANTISOCIAL))
            .enforcementCriminalDetails("")
            .build();

        // When
        AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData);

        // Then
        assertThat(response.getErrors()).containsExactly("Enter details");
    }

    @ParameterizedTest
    @MethodSource("validTextScenarios")
    void shouldAcceptValidText(String text, String description) {
        // Given
        PCSCase caseData = PCSCase.builder()
            .enforcementRiskCategories(Set.of(RiskCategory.CRIMINAL_OR_ANTISOCIAL))
            .enforcementCriminalDetails(text)
            .build();

        // When
        AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData);

        // Then
        assertThat(response.getErrors()).isNull();
        assertThat(response.getData().getEnforcementCriminalDetails()).isEqualTo(text);
    }

    @Test
    void shouldAcceptExactly6800Characters() {
        // Given
        String text = "a".repeat(6800);
        PCSCase caseData = PCSCase.builder()
            .enforcementRiskCategories(Set.of(RiskCategory.CRIMINAL_OR_ANTISOCIAL))
            .enforcementCriminalDetails(text)
            .build();

        // When
        AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData);

        // Then
        assertThat(response.getErrors()).isNull();
        assertThat(response.getData().getEnforcementCriminalDetails()).isEqualTo(text);
    }

    @Test
    void shouldRejectTextOver6800Characters() {
        // Given
        String longText = "a".repeat(6801);
        PCSCase caseData = PCSCase.builder()
            .enforcementRiskCategories(Set.of(RiskCategory.CRIMINAL_OR_ANTISOCIAL))
            .enforcementCriminalDetails(longText)
            .build();

        // When
        AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData);

        // Then
        assertThat(response.getErrors()).containsExactly(
            "In 'What is their history of criminal or antisocial behaviour?', you have entered more than the maximum number of characters (6800)"
        );
    }

    @Test
    void shouldRejectSignificantlyOverLimit() {
        // Given
        String longText = "a".repeat(7000);
        PCSCase caseData = PCSCase.builder()
            .enforcementRiskCategories(Set.of(RiskCategory.CRIMINAL_OR_ANTISOCIAL))
            .enforcementCriminalDetails(longText)
            .build();

        // When
        AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData);

        // Then
        assertThat(response.getErrors()).containsExactly(
            "In 'What is their history of criminal or antisocial behaviour?', you have entered more than the maximum number of characters (6800)"
        );
    }

    @Test
    void shouldPreserveDataWhenValid() {
        // Given
        String validText = "The defendant has a history of criminal and antisocial behaviour";
        PCSCase caseData = PCSCase.builder()
            .enforcementRiskCategories(Set.of(RiskCategory.CRIMINAL_OR_ANTISOCIAL))
            .enforcementCriminalDetails(validText)
            .enforcementViolentDetails("Some violent text")
            .enforcementFirearmsDetails("Some firearms text")
            .build();

        // When
        AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData);

        // Then
        assertThat(response.getErrors()).isNull();
        assertThat(response.getData().getEnforcementCriminalDetails()).isEqualTo(validText);
        assertThat(response.getData().getEnforcementViolentDetails()).isEqualTo("Some violent text");
        assertThat(response.getData().getEnforcementFirearmsDetails()).isEqualTo("Some firearms text");
    }

    private static Stream<Arguments> validTextScenarios() {
        return Stream.of(
            arguments("Short text", "Short description"),
            arguments("The defendant has a history of criminal and antisocial behaviour", "Medium description"),
            arguments("A".repeat(1000), "Long description (1000 chars)"),
            arguments("A".repeat(5000), "Very long description (5000 chars)"),
            arguments("A".repeat(6799), "Just under limit (6799 chars)")
        );
    }
}
