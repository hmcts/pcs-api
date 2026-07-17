package uk.gov.hmcts.reform.pcs.ccd.page.caseworker.removedocument;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.ccd.sdk.api.callback.AboutToStartOrSubmitResponse;
import uk.gov.hmcts.reform.pcs.ccd.domain.CaseFileCategory;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.domain.documentremoval.DocumentRemovalDetails;
import uk.gov.hmcts.reform.pcs.ccd.page.BasePageTest;
import uk.gov.hmcts.reform.pcs.ccd.service.TextAreaValidationService;
import uk.gov.hmcts.reform.pcs.ccd.service.document.DocumentAmendSelectionService;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SelectDocumentToRemovePageTest extends BasePageTest {

    @Mock
    private DocumentAmendSelectionService documentSelectionService;

    @BeforeEach
    void setUp() {
        setPageUnderTest(new SelectDocumentToRemovePage(documentSelectionService, new TextAreaValidationService()));
    }

    @Test
    void shouldCreateDocumentRemovalDetailsWhenAbsent() {
        PCSCase caseData = PCSCase.builder().build();

        callMidEventHandler(caseData);

        assertThat(caseData.getDocumentRemovalDetails()).isNotNull();
        verify(documentSelectionService).initialise(
            TEST_CASE_REFERENCE, caseData, caseData.getDocumentRemovalDetails());
    }

    @Test
    void shouldReuseExistingDocumentRemovalDetails() {
        DocumentRemovalDetails existingDetails = DocumentRemovalDetails.builder()
            .selectedFolder(CaseFileCategory.EVIDENCE)
            .build();
        PCSCase caseData = PCSCase.builder()
            .documentRemovalDetails(existingDetails)
            .build();

        callMidEventHandler(caseData);

        assertThat(caseData.getDocumentRemovalDetails()).isSameAs(existingDetails);
        verify(documentSelectionService).initialise(TEST_CASE_REFERENCE, caseData, existingDetails);
    }

    @Test
    void shouldSurfaceErrorsFromDocumentSelectionService() {
        PCSCase caseData = PCSCase.builder().build();
        when(documentSelectionService.validateAndStoreSelection(eq(caseData), any()))
            .thenReturn(List.of("Select a different folder to continue"));

        AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData);

        assertThat(response.getErrorMessageOverride()).isEqualTo("Select a different folder to continue");
    }

    @Test
    void shouldNotValidateReasonWhenNoFolderSelected() {
        PCSCase caseData = PCSCase.builder()
            .documentRemovalDetails(DocumentRemovalDetails.builder().build())
            .build();

        AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData);

        assertThat(response.getErrorMessageOverride()).isNull();
    }

    @Test
    void shouldValidateRemovalReasonLengthForSelectedDocument() {
        String tooLongReason = "a".repeat(TextAreaValidationService.SHORT_TEXT_LIMIT + 1);
        DocumentRemovalDetails details = DocumentRemovalDetails.builder()
            .selectedFolder(CaseFileCategory.EVIDENCE)
            .evidenceReason(tooLongReason)
            .build();
        PCSCase caseData = PCSCase.builder()
            .documentRemovalDetails(details)
            .build();

        AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData);

        assertThat(response.getErrorMessageOverride())
            .contains("Why are you removing this document?");
    }
}
