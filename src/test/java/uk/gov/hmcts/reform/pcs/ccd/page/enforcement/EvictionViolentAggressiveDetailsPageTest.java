package uk.gov.hmcts.reform.pcs.ccd.page.enforcement;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import uk.gov.hmcts.ccd.sdk.api.callback.AboutToStartOrSubmitResponse;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcement.EnforcementOrder;
import uk.gov.hmcts.reform.pcs.ccd.domain.RiskCategory;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.page.BasePageTest;

import java.util.Set;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;

class EvictionViolentAggressiveDetailsPageTest extends BasePageTest {

    @BeforeEach
    void setUp() {
        setPageUnderTest(new EvictionViolentAggressiveDetailsPage());
    }

    @Test
    void shouldRequireText() {
        // Given
        PCSCase caseData = PCSCase.builder()
            .enforcementOrder(EnforcementOrder.builder()
                .enforcementRiskCategories(Set.of(RiskCategory.VIOLENT_OR_AGGRESSIVE))
                .enforcementViolentDetails(null)
                .build())
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
            .enforcementOrder(EnforcementOrder.builder()
                .enforcementRiskCategories(Set.of(RiskCategory.VIOLENT_OR_AGGRESSIVE))
                .enforcementViolentDetails("   ")
                .build())
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
            .enforcementOrder(EnforcementOrder.builder()
                .enforcementRiskCategories(Set.of(RiskCategory.VIOLENT_OR_AGGRESSIVE))
                .enforcementViolentDetails("")
                .build())
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
            .enforcementOrder(EnforcementOrder.builder()
                .enforcementRiskCategories(Set.of(RiskCategory.VIOLENT_OR_AGGRESSIVE))
                .enforcementViolentDetails(text)
                .build())
            .build();

        // When
        AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData);

        // Then
        assertThat(response.getErrors()).isNull();
        assertThat(response.getData().getEnforcementOrder().getEnforcementViolentDetails()).isEqualTo(text);
    }

    @Test
    void shouldAcceptExactly6800Characters() {
        // Given
        String text = "a".repeat(6800);
        PCSCase caseData = PCSCase.builder()
            .enforcementOrder(EnforcementOrder.builder()
                .enforcementRiskCategories(Set.of(RiskCategory.VIOLENT_OR_AGGRESSIVE))
                .enforcementViolentDetails(text)
                .build())
            .build();

        // When
        AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData);

        // Then
        assertThat(response.getErrors()).isNull();
        assertThat(response.getData().getEnforcementOrder().getEnforcementViolentDetails()).isEqualTo(text);
    }

    @Test
    void shouldRejectTextOver6800Characters() {
        // Given
        String longText = "a".repeat(6801);
        PCSCase caseData = PCSCase.builder()
            .enforcementOrder(EnforcementOrder.builder()
                .enforcementRiskCategories(Set.of(RiskCategory.VIOLENT_OR_AGGRESSIVE))
                .enforcementViolentDetails(longText)
                .build())
            .build();

        // When
        AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData);

        // Then
        assertThat(response.getErrors()).containsExactly(
            "In 'How have they been violent or aggressive?', you have entered more than the "
                + "maximum number of characters (6800)"
        );
    }

    @Test
    void shouldRejectSignificantlyOverLimit() {
        // Given
        String longText = "a".repeat(7000);
        PCSCase caseData = PCSCase.builder()
            .enforcementOrder(EnforcementOrder.builder()
                .enforcementRiskCategories(Set.of(RiskCategory.VIOLENT_OR_AGGRESSIVE))
                .enforcementViolentDetails(longText)
                .build())
            .build();

        // When
        AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData);

        // Then
        assertThat(response.getErrors()).containsExactly(
            "In 'How have they been violent or aggressive?', you have entered more than the "
                + "maximum number of characters (6800)"
        );
    }

    @Test
    void shouldPreserveDataWhenValid() {
        // Given
        String validText = "The defendant has been violent on multiple occasions";
        PCSCase caseData = PCSCase.builder()
            .enforcementOrder(EnforcementOrder.builder()
                .enforcementRiskCategories(Set.of(RiskCategory.VIOLENT_OR_AGGRESSIVE))
                .enforcementViolentDetails(validText)
                .enforcementFirearmsDetails("Some firearms text")
                .enforcementCriminalDetails("Some criminal text")
                .build())
            .build();

        // When
        AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData);

        // Then
        assertThat(response.getErrors()).isNull();
        assertThat(response.getData().getEnforcementOrder().getEnforcementViolentDetails())
            .isEqualTo(validText);
        assertThat(response.getData().getEnforcementOrder().getEnforcementFirearmsDetails())
            .isEqualTo("Some firearms text");
        assertThat(response.getData().getEnforcementOrder().getEnforcementCriminalDetails())
            .isEqualTo("Some criminal text");
    }

    private static Stream<Arguments> validTextScenarios() {
        return Stream.of(
            arguments("Short text", "Short description"),
            arguments("The defendant has been violent on multiple occasions", "Medium description"),
            arguments("A".repeat(1000), "Long description (1000 chars)"),
            arguments("A".repeat(5000), "Very long description (5000 chars)"),
            arguments("A".repeat(6799), "Just under limit (6799 chars)")
        );
    }
}
