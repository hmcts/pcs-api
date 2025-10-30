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

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.hmcts.reform.pcs.ccd.domain.enforcement.PropertyAccessDetails.CLARIFICATION_PROPERTY_ACCESS_TEXT_LIMIT;

class PropertyAccessDetailsPageTest extends BasePageTest {

    private static final String SHORTEST_VALID_TEXT = "A";

    @BeforeEach
    void setUp() {
        setPageUnderTest(new PropertyAccessDetailsPage());
    }

    @Test
    void shouldAcceptValidShortestAllowedText() {
        // Given
        PCSCase caseData = PCSCase.builder()
                .enforcementOrder(EnforcementOrder.builder()
                        .propertyAccessDetails(PropertyAccessDetails.builder()
                                .propertyAccessYesNo(VerticalYesNo.YES)
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
                                .propertyAccessYesNo(VerticalYesNo.YES)
                                .clarificationOnAccessDifficultyText(
                                        SHORTEST_VALID_TEXT.repeat(CLARIFICATION_PROPERTY_ACCESS_TEXT_LIMIT))
                                .build())
                        .build())
                .build();

        // When
        AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData);

        // Then
        assertThat(response.getErrors()).isEmpty();
        assertThat(response.getData().getEnforcementOrder()
                .getPropertyAccessDetails().getClarificationOnAccessDifficultyText()).isEqualTo(
                        SHORTEST_VALID_TEXT.repeat(CLARIFICATION_PROPERTY_ACCESS_TEXT_LIMIT));
    }

    @Test
    void shouldRejectTextOver6800Characters() {
        // Given
        String longText = "a".repeat(6801);
        PCSCase caseData = PCSCase.builder()
                .enforcementOrder(EnforcementOrder.builder()
                        .propertyAccessDetails(PropertyAccessDetails.builder()
                                .propertyAccessYesNo(VerticalYesNo.YES)
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