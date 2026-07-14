package uk.gov.hmcts.reform.pcs.ccd.page.managehearing;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.ccd.sdk.api.callback.AboutToStartOrSubmitResponse;
import uk.gov.hmcts.reform.pcs.ccd.domain.hearing.Hearing;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.page.BasePageTest;
import uk.gov.hmcts.reform.pcs.ccd.service.TextAreaValidationService;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class AddHearingPageTest extends BasePageTest {

    @Mock
    private TextAreaValidationService textAreaValidationService;

    @BeforeEach
    void setUp() {
        lenient().doAnswer(invocation -> {
            Object caseData = invocation.getArgument(0);
            List<String> errors = invocation.getArgument(1);
            return AboutToStartOrSubmitResponse.<PCSCase, State>builder()
                .data((PCSCase) caseData)
                .errors(errors.isEmpty() ? null : errors)
                .build();
        }).when(textAreaValidationService).createValidationResponse(any(), any());
        setPageUnderTest(new AddHearingPage(textAreaValidationService));
    }

    @Test
    void shouldValidateNotesAndAdditionalInformation() {
        // Given
        String notes = "notes";
        String additionalInformation = "additional information";
        Hearing hearing = Hearing.builder()
            .notes(notes)
            .additionalInformation(additionalInformation)
            .build();

        PCSCase caseData = PCSCase.builder()
            .hearing(hearing)
            .build();

        // When
        AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData);

        // Then
        assertThat(response.getErrorMessageOverride()).isNull();
        verify(textAreaValidationService).validateMultipleTextAreas(
            argThat(f -> f.fieldValue.equals(notes)
                && f.fieldLabel.equals("Hearing notes")
                && f.maxCharacters == 500),

            argThat(f -> f.fieldValue.equals(additionalInformation)
                && f.fieldLabel.equals("Enter any additional information")
                && f.maxCharacters == 500)
        );
    }
}
