package uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim.wales;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.ccd.sdk.api.callback.AboutToStartOrSubmitResponse;
import uk.gov.hmcts.ccd.sdk.type.Document;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.domain.wales.WalesDocuments;
import uk.gov.hmcts.reform.pcs.ccd.page.BasePageTest;
import uk.gov.hmcts.reform.pcs.ccd.service.FileUploadValidationService;
import uk.gov.hmcts.reform.pcs.ccd.service.TextAreaValidationService;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static uk.gov.hmcts.reform.pcs.ccd.service.FileUploadValidationService.ALLOWED_FILE_TYPE_GUIDANCE;
import static uk.gov.hmcts.reform.pcs.ccd.service.FileUploadValidationService.DISALLOWED_FILE_TYPE_ERROR;
import static uk.gov.hmcts.reform.pcs.ccd.util.ListValueUtils.wrapListItems;

@ExtendWith(MockitoExtension.class)
class UploadRequiredDocumentsWalesTest extends BasePageTest {
    @Mock
    private TextAreaValidationService textAreaValidationService;

    @BeforeEach
    void setUp() {
        lenient().doReturn(new ArrayList<>()).when(textAreaValidationService)
            .validateMultipleTextAreas(any(), any());
        doAnswer(invocation -> {
            Object caseData = invocation.getArgument(0);
            List<String> errors = invocation.getArgument(1);
            return AboutToStartOrSubmitResponse.<PCSCase, State>builder()
                .data((PCSCase) caseData)
                .errors(errors.isEmpty() ? null : errors)
                .build();
        }).when(textAreaValidationService).createValidationResponse(any(), anyList());

        setPageUnderTest(new UploadRequiredDocumentsWales(
            textAreaValidationService, new FileUploadValidationService()));
    }

    @Test
    void shouldValidateUploadRequiredDocumentsWalesInputs() {
        // Given
        String reason1 = "reasons1";
        String reason2 = "reasons2";
        String reason3 = "reasons3";
        PCSCase caseData = PCSCase.builder()
            .requiredDocumentsWales(
                WalesDocuments.builder()
                    .noEpcReason(reason1)
                    .noGasReportReason(reason2)
                    .noEicrReason(reason3)
                    .build()
            )
            .build();

        // when
        callMidEventHandler(caseData);

        verify(textAreaValidationService).validateMultipleTextAreas(
            argThat(f -> f.fieldValue.equals(reason1)
                && f.fieldLabel.equals("Why can you not upload a copy of the energy performance certificate?")
                && f.maxCharacters == 500),

            argThat(f -> f.fieldValue.equals(reason2)
                && f.fieldLabel.equals("Why can you not upload a copy of the gas safety report?")
                && f.maxCharacters == 500),

            argThat(f -> f.fieldValue.equals(reason3)
                && f.fieldLabel.equals(
                    "Why can you not upload a copy of the current Electrical Installation Condition Report (EICR)?")
                && f.maxCharacters == 500)
        );

    }

    @Test
    void shouldReturnErrorWhenARequiredDocumentIsDisallowedFileType() {
        // Given
        PCSCase caseData = PCSCase.builder()
            .requiredDocumentsWales(
                WalesDocuments.builder()
                    .energyPerformance(wrapListItems(List.of(
                        Document.builder().filename("epc.pdf").build())))
                    .gasSafetyReport(wrapListItems(List.of(
                        Document.builder().filename("gas-report.mpg").build())))
                    .build()
            )
            .build();

        // When
        AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData);

        // Then
        assertThat(response.getErrors()).containsExactly(DISALLOWED_FILE_TYPE_ERROR, ALLOWED_FILE_TYPE_GUIDANCE);
    }

    @Test
    void shouldNotReturnErrorWhenAllRequiredDocumentsAreAllowedFileTypes() {
        // Given
        PCSCase caseData = PCSCase.builder()
            .requiredDocumentsWales(
                WalesDocuments.builder()
                    .energyPerformance(wrapListItems(List.of(
                        Document.builder().filename("epc.pdf").build())))
                    .gasSafetyReport(wrapListItems(List.of(
                        Document.builder().filename("gas-report.pdf").build())))
                    .electricalInstallation(wrapListItems(List.of(
                        Document.builder().filename("eicr.pdf").build())))
                    .build()
            )
            .build();

        // When
        AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData);

        // Then
        assertThat(response.getErrors()).isNull();
    }
}
