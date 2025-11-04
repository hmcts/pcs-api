package uk.gov.hmcts.reform.pcs.ccd.page.enforcement;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import uk.gov.hmcts.ccd.sdk.api.callback.AboutToStartOrSubmitResponse;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcement.EnforcementOrder;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcement.EnforcementRiskDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcement.RiskCategory;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.page.BasePageTest;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class FirearmsPossessionRiskPageTest extends BasePageTest {

    @BeforeEach
    void setUp() {
        setPageUnderTest(new FirearmsPossessionRiskPage());
    }

    @ParameterizedTest
    @MethodSource("uk.gov.hmcts.reform.pcs.ccd.page.enforcement.RiskCategoryTestUtil#validTextScenarios")
    void shouldAcceptValidText(String text) {
        // Given
        PCSCase caseData = PCSCase.builder()
            .enforcementOrder(EnforcementOrder.builder()
                .enforcementRiskCategories(Set.of(RiskCategory.FIREARMS_POSSESSION))
                .riskDetails(EnforcementRiskDetails.builder()
                    .enforcementFirearmsDetails(text)
                    .build())
                .build())
            .build();

        // When
        AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData);

        // Then
        assertThat(response.getErrors()).isEmpty();
        assertThat(response.getData().getEnforcementOrder()
            .getRiskDetails().getEnforcementFirearmsDetails()).isEqualTo(text);
    }

    @Test
    void shouldRejectTextOver6800Characters() {
        // Given
        String longText = "a".repeat(6801);
        PCSCase caseData = PCSCase.builder()
            .enforcementOrder(EnforcementOrder.builder()
                .enforcementRiskCategories(Set.of(RiskCategory.FIREARMS_POSSESSION))
                .riskDetails(EnforcementRiskDetails.builder()
                    .enforcementFirearmsDetails(longText)
                    .build())
                .build())
            .build();

        // When
        AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData);

        // Then
        assertThat(response.getErrors()).containsExactly(
            EnforcementRiskValidationUtils.getCharacterLimitErrorMessage(RiskCategory.FIREARMS_POSSESSION)
        );
    }


    @Test
    void shouldPreserveDataWhenValid() {
        // Given
        String validText = "The defendant has a history of firearm possession";
        PCSCase caseData = PCSCase.builder()
            .enforcementOrder(EnforcementOrder.builder()
                .enforcementRiskCategories(Set.of(RiskCategory.FIREARMS_POSSESSION))
                .riskDetails(EnforcementRiskDetails.builder()
                    .enforcementFirearmsDetails(validText)
                    .enforcementViolentDetails("Some violent text")
                    .enforcementCriminalDetails("Some criminal text")
                    .build())
                .build())
            .build();

        // When
        AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData);

        // Then
        assertThat(response.getErrors()).isEmpty();
        assertThat(response.getData().getEnforcementOrder()
            .getRiskDetails().getEnforcementFirearmsDetails())
            .isEqualTo(validText);
        assertThat(response.getData().getEnforcementOrder()
            .getRiskDetails().getEnforcementViolentDetails())
            .isEqualTo("Some violent text");
        assertThat(response.getData().getEnforcementOrder()
            .getRiskDetails().getEnforcementCriminalDetails())
            .isEqualTo("Some criminal text");
    }
}
