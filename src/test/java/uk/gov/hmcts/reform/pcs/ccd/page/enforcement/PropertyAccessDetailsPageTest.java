package uk.gov.hmcts.reform.pcs.ccd.page.enforcement;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import uk.gov.hmcts.ccd.sdk.api.callback.AboutToStartOrSubmitResponse;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcement.EnforcementOrder;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcement.PropertyAccessDetails;
import uk.gov.hmcts.reform.pcs.ccd.page.BasePageTest;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.hmcts.reform.pcs.ccd.domain.enforcement.PropertyAccessDetails.CLARIFICATION_PROPERTY_ACCESS_TEXT_LIMIT;

class PropertyAccessDetailsPageTest extends BasePageTest {

    @BeforeEach
    void setUp() {
        setPageUnderTest(new PropertyAccessDetailsPage());
    }

    @ParameterizedTest
    @MethodSource("uk.gov.hmcts.reform.pcs.ccd.page.enforcement.EnforcementTestUtil#validTextScenarios")
    void shouldAcceptValidText(String text) {
        // Given
        PCSCase caseData = PCSCase.builder()
                .enforcementOrder(EnforcementOrder.builder()
                        .propertyAccessDetails(PropertyAccessDetails.builder()
                                .clarificationOnAccessDifficultyText(text)
                                .build())
                        .build())
                .build();

        // When
        AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData);

        // Then
        assertThat(response.getErrors()).isEmpty();
        assertThat(response.getData().getEnforcementOrder()
                .getPropertyAccessDetails().getClarificationOnAccessDifficultyText()).isEqualTo(text);
    }

    @Test
    void shouldRejectTextOver6800Characters() {
        // Given
        String longText = "a".repeat(6801);
        PCSCase caseData = PCSCase.builder()
                .enforcementOrder(EnforcementOrder.builder()
                        .propertyAccessDetails(PropertyAccessDetails.builder()
                                .clarificationOnAccessDifficultyText(longText)
                                .build())
                        .build())
                .build();

        // When
        AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData);

        // Then
        assertThat(response.getErrors()).containsExactly(
                EnforcementValidationUtil.getCharacterLimitErrorMessage(
                        PropertyAccessDetails.CLARIFICATION_PROPERTY_ACCESS_LABEL,
                        CLARIFICATION_PROPERTY_ACCESS_TEXT_LIMIT)
        );
    }
}