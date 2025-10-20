package uk.gov.hmcts.reform.pcs.ccd.page.enforcement;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import uk.gov.hmcts.ccd.sdk.api.callback.AboutToStartOrSubmitResponse;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcement.EnforcementOrder;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcement.RiskCategory;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.page.BasePageTest;

import java.util.Set;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class ViolentAggressiveRiskTest extends BasePageTest {

    @BeforeEach
    void setUp() {
        setPageUnderTest(new ViolentAggressiveRisk());
    }

    @ParameterizedTest
    @MethodSource("invalidTextScenarios")
    void shouldRequireTextWhenInvalid(String invalidText) {
        // Given
        PCSCase caseData = PCSCase.builder()
            .enforcementOrder(EnforcementOrder.builder()
                .enforcementRiskCategories(Set.of(RiskCategory.VIOLENT_OR_AGGRESSIVE))
                .riskDetails(uk.gov.hmcts.reform.pcs.ccd.domain.enforcement.EnforcementRiskDetails.builder()
                    .enforcementViolentDetails(invalidText)
                    .build())
                .build())
            .build();

        // When
        AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData);

        // Then
        assertThat(response.getErrors()).containsExactly("Enter details");
    }

    @ParameterizedTest
    @MethodSource("validTextScenarios")
    void shouldAcceptValidText(String text) {
        // Given
        PCSCase caseData = PCSCase.builder()
            .enforcementOrder(EnforcementOrder.builder()
                .enforcementRiskCategories(Set.of(RiskCategory.VIOLENT_OR_AGGRESSIVE))
                .riskDetails(uk.gov.hmcts.reform.pcs.ccd.domain.enforcement.EnforcementRiskDetails.builder()
                    .enforcementViolentDetails(text)
                    .build())
                .build())
            .build();

        // When
        AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData);

        // Then
        assertThat(response.getErrors()).isNull();
        assertThat(response.getData().getEnforcementOrder()
            .getRiskDetails().getEnforcementViolentDetails()).isEqualTo(text);
    }

    @Test
    void shouldAcceptExactly6800Characters() {
        // Given
        String text = "a".repeat(6800);
        PCSCase caseData = PCSCase.builder()
            .enforcementOrder(EnforcementOrder.builder()
                .enforcementRiskCategories(Set.of(RiskCategory.VIOLENT_OR_AGGRESSIVE))
                .riskDetails(uk.gov.hmcts.reform.pcs.ccd.domain.enforcement.EnforcementRiskDetails.builder()
                    .enforcementViolentDetails(text)
                    .build())
                .build())
            .build();

        // When
        AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData);

        // Then
        assertThat(response.getErrors()).isNull();
        assertThat(response.getData().getEnforcementOrder()
            .getRiskDetails().getEnforcementViolentDetails()).isEqualTo(text);
    }

    @Test
    void shouldRejectTextOver6800Characters() {
        // Given
        String longText = "a".repeat(6801);
        PCSCase caseData = PCSCase.builder()
            .enforcementOrder(EnforcementOrder.builder()
                .enforcementRiskCategories(Set.of(RiskCategory.VIOLENT_OR_AGGRESSIVE))
                .riskDetails(uk.gov.hmcts.reform.pcs.ccd.domain.enforcement.EnforcementRiskDetails.builder()
                    .enforcementViolentDetails(longText)
                    .build())
                .build())
            .build();

        // When
        AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData);

        // Then
        assertThat(response.getErrors()).containsExactly(
            ViolentAggressiveRisk.buildCharacterLimitError()
        );
    }

    @Test
    void shouldRejectSignificantlyOverLimit() {
        // Given
        String longText = "a".repeat(7000);
        PCSCase caseData = PCSCase.builder()
            .enforcementOrder(EnforcementOrder.builder()
                .enforcementRiskCategories(Set.of(RiskCategory.VIOLENT_OR_AGGRESSIVE))
                .riskDetails(uk.gov.hmcts.reform.pcs.ccd.domain.enforcement.EnforcementRiskDetails.builder()
                    .enforcementViolentDetails(longText)
                    .build())
                .build())
            .build();

        // When
        AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData);

        // Then
        assertThat(response.getErrors()).containsExactly(
            ViolentAggressiveRisk.buildCharacterLimitError()
        );
    }

    @Test
    void shouldPreserveDataWhenValid() {
        // Given
        String validText = "The defendant has been violent on multiple occasions";
        PCSCase caseData = PCSCase.builder()
            .enforcementOrder(EnforcementOrder.builder()
                .enforcementRiskCategories(Set.of(RiskCategory.VIOLENT_OR_AGGRESSIVE))
                .riskDetails(uk.gov.hmcts.reform.pcs.ccd.domain.enforcement.EnforcementRiskDetails.builder()
                    .enforcementViolentDetails(validText)
                    .enforcementFirearmsDetails("Some firearms text")
                    .enforcementCriminalDetails("Some criminal text")
                    .build())
                .build())
            .build();

        // When
        AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData);

        // Then
        assertThat(response.getErrors()).isNull();
        assertThat(response.getData().getEnforcementOrder()
            .getRiskDetails().getEnforcementViolentDetails())
            .isEqualTo(validText);
        assertThat(response.getData().getEnforcementOrder()
            .getRiskDetails().getEnforcementFirearmsDetails())
            .isEqualTo("Some firearms text");
        assertThat(response.getData().getEnforcementOrder()
            .getRiskDetails().getEnforcementCriminalDetails())
            .isEqualTo("Some criminal text");
    }

    private static Stream<String> validTextScenarios() {
        return Stream.of(
            "Short text",
            "The defendant has been violent on multiple occasions",
            "A".repeat(1000),
            "A".repeat(5000),
            "A".repeat(6799)
        );
    }

    private static Stream<String> invalidTextScenarios() {
        return Stream.of(
            (String) null,
            "   ",
            ""
        );
    }
}
