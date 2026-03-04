package uk.gov.hmcts.reform.pcs.ccd.page.enforcetheorder.warrantofrestitution;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.ccd.sdk.api.callback.AboutToStartOrSubmitResponse;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.EnforcementOrder;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.common.PropertyAccessDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.warrantofrestitution.WarrantOfRestitutionDetails;
import uk.gov.hmcts.reform.pcs.ccd.page.BasePageTest;
import uk.gov.hmcts.reform.pcs.ccd.service.TextAreaValidationService;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.hmcts.reform.pcs.ccd.service.TextAreaValidationService.CHARACTER_LIMIT_ERROR_TEMPLATE;

class PropertyAccessDetailsWarrantOfRestitutionPageTest extends BasePageTest {

    @BeforeEach
    void setUp() {
        TextAreaValidationService textAreaValidationService = new TextAreaValidationService();
        setPageUnderTest(new PropertyAccessDetailsWarrantOfRestitutionPage(textAreaValidationService));
    }

    @Test
    void shouldAcceptValidText() {
        // Given
        String validText = "Some property access details";
        PCSCase caseData = PCSCase.builder()
                .enforcementOrder(EnforcementOrder.builder()
                    .warrantOfRestitutionDetails(WarrantOfRestitutionDetails.builder()
                        .propertyAccessDetails(PropertyAccessDetails.builder()
                                .isDifficultToAccessProperty(VerticalYesNo.YES)
                                .clarificationOnAccessDifficultyText(validText)
                                .build())
                        .build())
                    .build())
                .build();

        // When
        AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData);

        // Then
        assertThat(response.getErrorMessageOverride()).isNullOrEmpty();
        assertThat(response.getErrors()).isNullOrEmpty();
        assertThat(response.getData().getEnforcementOrder().getWarrantOfRestitutionDetails()
                .getPropertyAccessDetails().getClarificationOnAccessDifficultyText()).isEqualTo(
                        validText);
    }

    @Test
    void shouldRejectTextOver6800Characters() {
        // Given
        String longText = "a".repeat(6801);
        PCSCase caseData = PCSCase.builder()
                .enforcementOrder(EnforcementOrder.builder()
                    .warrantOfRestitutionDetails(WarrantOfRestitutionDetails.builder()
                        .propertyAccessDetails(PropertyAccessDetails.builder()
                                .isDifficultToAccessProperty(VerticalYesNo.YES)
                                .clarificationOnAccessDifficultyText(longText)
                                .build())
                        .build())
                    .build())
                .build();

        // When
        AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData);

        // Then
        String expectedError = String.format(CHARACTER_LIMIT_ERROR_TEMPLATE,
                                             "Explain why it’s difficult to access the property",
                                             "6,800");

        assertThat(response.getErrorMessageOverride()).isEqualTo(expectedError);
    }
}
