package uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.ccd.sdk.api.callback.AboutToStartOrSubmitResponse;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.RentArrearsSection;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.page.BasePageTest;
import uk.gov.hmcts.reform.pcs.ccd.service.FileUploadValidationService;
import uk.gov.hmcts.reform.pcs.ccd.service.TextAreaValidationService;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static uk.gov.hmcts.reform.pcs.ccd.service.FileUploadValidationService.ALLOWED_FILE_TYPE_GUIDANCE;
import static uk.gov.hmcts.reform.pcs.ccd.service.FileUploadValidationService.DISALLOWED_FILE_TYPE_ERROR;
import static uk.gov.hmcts.reform.pcs.ccd.service.FileUploadValidationService.RENT_STATEMENT_REQUIRED;
import static uk.gov.hmcts.reform.pcs.ccd.testutil.DocumentTestData.documentsWithFilenames;

@ExtendWith(MockitoExtension.class)
public class RentArrearsTest extends BasePageTest {

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
        }).when(textAreaValidationService).createValidationResponse(any(), anyList());
        setPageUnderTest(new RentArrears(textAreaValidationService, new FileUploadValidationService()));
    }

    @Test
    void shouldValidateRentArrearsRecoveryAttemptDetailsInput() {
        // Given
        String rentRecoveryAttempt = "rent recovery attempt";
        String label = "Give details of previous steps taken to recover rent arrears";
        Integer characterLimit = 500;
        PCSCase caseData = PCSCase.builder()
            .rentArrears(
                RentArrearsSection
                    .builder()
                    .recoveryAttemptDetails(rentRecoveryAttempt)
                    .build()
            ).build();

        // When
        AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData);

        // Then
        assertThat(response.getErrorMessageOverride()).isNull();
        verify(textAreaValidationService, times(1))
            .validateSingleTextArea(eq(rentRecoveryAttempt), eq(label), eq(characterLimit));
    }

    @Test
    void shouldReturnErrorWhenRentStatementDocumentIsDisallowedFileType() {
        // Given
        PCSCase caseData = PCSCase.builder()
            .rentArrears(
                RentArrearsSection.builder()
                    .statementDocuments(documentsWithFilenames("rent-statement.mp3"))
                    .build()
            ).build();

        // When
        AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData);

        // Then
        assertThat(response.getErrors()).containsExactly(DISALLOWED_FILE_TYPE_ERROR, ALLOWED_FILE_TYPE_GUIDANCE);
    }

    @Test
    void shouldReturnRequiredErrorWhenNoRentStatementUploaded() {
        // Given
        PCSCase caseData = PCSCase.builder()
            .rentArrears(RentArrearsSection.builder().build())
            .build();

        // When
        AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData);

        // Then
        assertThat(response.getErrors()).containsExactly(RENT_STATEMENT_REQUIRED);
    }

    @Test
    void shouldNotReturnErrorWhenRentStatementDocumentIsAllowedFileType() {
        // Given
        PCSCase caseData = PCSCase.builder()
            .rentArrears(
                RentArrearsSection.builder()
                    .statementDocuments(documentsWithFilenames("rent-statement.pdf"))
                    .build()
            ).build();

        // When
        AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData);

        // Then
        assertThat(response.getErrors()).isNull();
    }
}
