package uk.gov.hmcts.reform.pcs.ccd.page.enforcement;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import uk.gov.hmcts.ccd.sdk.api.callback.AboutToStartOrSubmitResponse;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcement.EnforcementOrder;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcement.EnforcementRiskDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcement.RiskCategory;
import uk.gov.hmcts.reform.pcs.ccd.page.BasePageTest;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class VerbalOrWrittenThreatsRiskPageTest extends BasePageTest {

    @BeforeEach
    void setUp() {
        setPageUnderTest(new VerbalOrWrittenThreatsRiskPage());
    }

    @ParameterizedTest
    @MethodSource("uk.gov.hmcts.reform.pcs.ccd.page.enforcement.RiskCategoryTestUtil#invalidTextScenarios")
    void shouldRequireTextWhenInvalid(String invalidText) {
        // Given
        PCSCase caseData = PCSCase.builder()
                .enforcementOrder(EnforcementOrder.builder()
                        .enforcementRiskCategories(Set.of(RiskCategory.VERBAL_OR_WRITTEN_THREATS))
                        .riskDetails(EnforcementRiskDetails.builder()
                                .enforcementVerbalOrWrittenThreatsDetails(invalidText)
                                .build())
                        .build())
                .build();

        // When
        AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData);

        // Then
        assertThat(response.getErrors()).containsExactly("Enter details");
    }

    @ParameterizedTest
    @MethodSource("uk.gov.hmcts.reform.pcs.ccd.page.enforcement.RiskCategoryTestUtil#validTextScenarios")
    void shouldAcceptValidText(String text) {
        // Given
        PCSCase caseData = PCSCase.builder()
                .enforcementOrder(EnforcementOrder.builder()
                        .enforcementRiskCategories(Set.of(RiskCategory.VERBAL_OR_WRITTEN_THREATS))
                        .riskDetails(EnforcementRiskDetails.builder()
                                .enforcementVerbalOrWrittenThreatsDetails(text)
                                .build())
                        .build())
                .build();

        // When
        AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData);

        // Then
        assertThat(response.getErrors()).isEmpty();
        assertThat(response.getData().getEnforcementOrder()
                .getRiskDetails().getEnforcementVerbalOrWrittenThreatsDetails()).isEqualTo(text);
    }

    @Test
    void shouldRejectTextOver6800Characters() {
        // Given
        String longText = "a".repeat(6801);
        PCSCase caseData = PCSCase.builder()
                .enforcementOrder(EnforcementOrder.builder()
                        .enforcementRiskCategories(Set.of(RiskCategory.VERBAL_OR_WRITTEN_THREATS))
                        .riskDetails(EnforcementRiskDetails.builder()
                                .enforcementVerbalOrWrittenThreatsDetails(longText)
                                .build())
                        .build())
                .build();

        // When
        AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData);

        // Then
        assertThat(response.getErrors()).containsExactly(
                EnforcementRiskValidationUtils.getCharacterLimitErrorMessage(RiskCategory.VERBAL_OR_WRITTEN_THREATS)
        );
    }
}
