package uk.gov.hmcts.reform.pcs.ccd.page.entergenapp;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.ccd.sdk.api.callback.AboutToStartOrSubmitResponse;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.domain.genapp.EnterGenAppRequest;
import uk.gov.hmcts.reform.pcs.ccd.domain.genapp.EnterGenAppType;
import uk.gov.hmcts.reform.pcs.ccd.page.BasePageTest;
import uk.gov.hmcts.reform.pcs.ccd.service.TextAreaValidationService;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.hmcts.reform.pcs.ccd.service.TextAreaValidationService.CHARACTER_LIMIT_ERROR_TEMPLATE;
import static uk.gov.hmcts.reform.pcs.ccd.service.TextAreaValidationService.MEDIUM_TEXT_LIMIT;

@ExtendWith(MockitoExtension.class)
class ApplicationDetailsTest extends BasePageTest {

    @BeforeEach
    void setUp() {
        TextAreaValidationService textAreaValidationService = new TextAreaValidationService();
        setPageUnderTest(new ApplicationDetails(textAreaValidationService));
    }

    @Test
    void shouldRejectSomethingElseDetailsOverCharacterLimit() {
        // Given
        String longText = "a".repeat(MEDIUM_TEXT_LIMIT + 1);
        PCSCase caseData = PCSCase.builder()
            .enterGenAppRequest(EnterGenAppRequest.builder()
                .applicationTypeOption(EnterGenAppType.SOMETHING_ELSE)
                .somethingElseDetails(longText)
                .build())
            .build();

        // When
        AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData);

        // Then
        String expectedError = String.format(
            CHARACTER_LIMIT_ERROR_TEMPLATE,
            "Which categories apply",
            "500"
        );
        assertThat(response.getErrorMessageOverride()).isEqualTo(expectedError);
    }

    @Test
    void shouldNotValidateSomethingElseDetailsForOtherApplicationTypes() {
        // Given
        String longText = "a".repeat(MEDIUM_TEXT_LIMIT + 1);
        PCSCase caseData = PCSCase.builder()
            .enterGenAppRequest(EnterGenAppRequest.builder()
                .applicationTypeOption(EnterGenAppType.ADJOURN)
                .somethingElseDetails(longText)
                .build())
            .build();

        // When
        AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData);

        // Then
        assertThat(response.getErrorMessageOverride()).isNull();
    }

}
