package uk.gov.hmcts.reform.pcs.ccd.page.managehearing;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.ccd.sdk.api.callback.AboutToStartOrSubmitResponse;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.domain.hearing.Hearing;
import uk.gov.hmcts.reform.pcs.ccd.page.BasePageTest;
import uk.gov.hmcts.reform.pcs.ccd.service.TextAreaValidationService;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CancelHearingPageTest extends BasePageTest {

    @Mock
    private TextAreaValidationService textAreaValidationService;

    @BeforeEach
    void setUp() {
        CancelHearingPage cancelHearingPage = new CancelHearingPage(textAreaValidationService);
        setPageUnderTest(cancelHearingPage);
    }

    @Test
    void shouldValidateTextAreaLength() {
        // Given
        String cancellationReason = "some cancellation reason";
        PCSCase caseData = PCSCase.builder()
            .hearing(Hearing.builder()
                         .cancellationReason(cancellationReason)
                         .build())
            .build();

        List<String> validationErrors = List.of("Error 1", "Error 2");
        when(textAreaValidationService
                 .validateSingleTextArea(cancellationReason, "Enter reason for cancellation", 500))
            .thenReturn(validationErrors);

        AboutToStartOrSubmitResponse<PCSCase, State> expectedResponse = createMockResponse();
        when(textAreaValidationService.<PCSCase, State>createValidationResponse(caseData, validationErrors))
            .thenReturn(expectedResponse);

        // When
        AboutToStartOrSubmitResponse<PCSCase, State> actualResponse = callMidEventHandler(caseData);

        // Then
        assertThat(actualResponse).isEqualTo(expectedResponse);
    }

    @SuppressWarnings("unchecked")
    private static AboutToStartOrSubmitResponse<PCSCase, State> createMockResponse() {
        return mock(AboutToStartOrSubmitResponse.class);
    }
}
