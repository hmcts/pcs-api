package uk.gov.hmcts.reform.pcs.ccd.page.enforcement;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.ccd.sdk.api.callback.AboutToStartOrSubmitResponse;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcement.EnforcementOrder;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcement.PropertyAccessDetails;
import uk.gov.hmcts.reform.pcs.ccd.page.BasePageTest;
import uk.gov.hmcts.reform.pcs.ccd.service.TextAreaValidationService;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.hmcts.reform.pcs.ccd.service.TextAreaValidationService.CHARACTER_LIMIT_ERROR_TEMPLATE;

class PropertyAccessDetailsPageTest extends BasePageTest {

    private static final String SHORTEST_VALID_TEXT = "A";

    @BeforeEach
    void setUp() {
        TextAreaValidationService textAreaValidationService = new TextAreaValidationService();
        setPageUnderTest(new PropertyAccessDetailsPage(textAreaValidationService));
    }

    @Test
    void shouldAcceptValidShortestAllowedText() {
        // Given
        PCSCase caseData = PCSCase.builder()
                .enforcementOrder(EnforcementOrder.builder()
                        .propertyAccessDetails(PropertyAccessDetails.builder()
                                .isDifficultToAccessProperty(VerticalYesNo.YES)
                                .clarificationOnAccessDifficultyText(SHORTEST_VALID_TEXT)
                                .build())
                        .build())
                .build();

        // When
        AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData);

        // Then
        assertThat(response.getErrors()).isEmpty();
        assertThat(response.getData().getEnforcementOrder()
                .getPropertyAccessDetails().getClarificationOnAccessDifficultyText())
                .isEqualTo(SHORTEST_VALID_TEXT);
    }

    @Test
    void shouldAcceptValidLongestAllowedText() {
        // Given
        PCSCase caseData = PCSCase.builder()
                .enforcementOrder(EnforcementOrder.builder()
                        .propertyAccessDetails(PropertyAccessDetails.builder()
                                .isDifficultToAccessProperty(VerticalYesNo.YES)
                                .clarificationOnAccessDifficultyText(
                                        SHORTEST_VALID_TEXT.repeat(6800))
                                .build())
                        .build())
                .build();

        // When
        AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData);

        // Then
        assertThat(response.getErrors()).isEmpty();
        assertThat(response.getData().getEnforcementOrder()
                .getPropertyAccessDetails().getClarificationOnAccessDifficultyText()).isEqualTo(
                        SHORTEST_VALID_TEXT.repeat(6800));
    }

    @Test
    void shouldRejectTextOver6800Characters() {
        // Given
        String longText = "a".repeat(6801);
        PCSCase caseData = PCSCase.builder()
                .enforcementOrder(EnforcementOrder.builder()
                        .propertyAccessDetails(PropertyAccessDetails.builder()
                                .isDifficultToAccessProperty(VerticalYesNo.YES)
                                .clarificationOnAccessDifficultyText(longText)
                                .build())
                        .build())
                .build();

        // When
        AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData);

        // Then
        String expectedError = String.format(CHARACTER_LIMIT_ERROR_TEMPLATE,
                                             "Explain why it’s difficult to access the property",
                                             "6,800");

        assertThat(response.getErrors()).containsExactly(expectedError);
    }
}
