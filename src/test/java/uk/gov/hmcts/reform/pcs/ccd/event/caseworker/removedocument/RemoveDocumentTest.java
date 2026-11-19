package uk.gov.hmcts.reform.pcs.ccd.event.caseworker.removedocument;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.ccd.sdk.api.callback.SubmitResponse;
import uk.gov.hmcts.reform.pcs.ccd.domain.CaseFileCategory;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.domain.documentremoval.DocumentRemovalDetails;
import uk.gov.hmcts.reform.pcs.ccd.event.BaseEventTest;
import uk.gov.hmcts.reform.pcs.ccd.page.caseworker.removedocument.SelectDocumentToRemovePage;
import uk.gov.hmcts.reform.pcs.ccd.service.document.DocumentRemovalService;
import uk.gov.hmcts.reform.pcs.ccd.service.document.DocumentSelectionService;
import uk.gov.hmcts.reform.pcs.ccd.util.ConfirmationPanelBuilder;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RemoveDocumentTest extends BaseEventTest {

    @Mock
    private DocumentSelectionService documentSelectionService;
    @Mock
    private DocumentRemovalService documentRemovalService;
    @Mock
    private SelectDocumentToRemovePage selectDocumentToRemovePage;
    @Mock
    private ConfirmationPanelBuilder confirmationPanelBuilder;

    @BeforeEach
    void setUp() {
        RemoveDocument underTest = new RemoveDocument(
            documentSelectionService, documentRemovalService, confirmationPanelBuilder, selectDocumentToRemovePage
        );

        setEventUnderTest(underTest);
    }

    @Test
    void shouldCreateDocumentRemovalDetailsWhenAbsentOnStart() {
        PCSCase caseData = PCSCase.builder().build();

        PCSCase result = callStartHandler(caseData);

        assertThat(result.getDocumentRemovalDetails()).isNotNull();
        verify(documentSelectionService).initialise(
            eq(TEST_CASE_REFERENCE), eq(result), eq(result.getDocumentRemovalDetails()));
    }

    @Test
    void shouldReuseExistingDocumentRemovalDetailsOnStart() {
        DocumentRemovalDetails existingDetails = DocumentRemovalDetails.builder()
            .selectedFolder(CaseFileCategory.EVIDENCE)
            .build();
        PCSCase caseData = PCSCase.builder()
            .documentRemovalDetails(existingDetails)
            .build();

        PCSCase result = callStartHandler(caseData);

        assertThat(result.getDocumentRemovalDetails()).isSameAs(existingDetails);
        verify(documentSelectionService).initialise(TEST_CASE_REFERENCE, result, existingDetails);
    }

    @Test
    void shouldBuildConfirmationPageWithRemovedDocumentDetails() {
        // Given
        UUID documentId = UUID.randomUUID();
        String reason = "No longer required";
        DocumentRemovalDetails details = DocumentRemovalDetails.builder()
            .selectedFolder(CaseFileCategory.EVIDENCE)
            .selectedDocumentId(documentId.toString())
            .selectedDocumentFileName("evidence.pdf")
            .evidenceReason(reason)
            .build();
        PCSCase caseData = PCSCase.builder()
            .documentRemovalDetails(details)
            .caseNameHmctsInternal("Smith v Jones")
            .build();

        String confirmationPanelMarkup = stubConfirmationPanelBuilder();

        // When
        SubmitResponse<State> response = callSubmitHandler(caseData);

        // Then
        verify(documentRemovalService).removeDocument(documentId, reason);
        verify(confirmationPanelBuilder)
            .buildStandardConfirmationPanel("evidence.pdf removed", caseData, TEST_CASE_REFERENCE);

        assertThat(response.getConfirmationBody())
            .contains(confirmationPanelMarkup);
    }

    private String stubConfirmationPanelBuilder() {
        String confirmationPanelMarkup = "confirmation panel markup";
        when(confirmationPanelBuilder
                 .buildStandardConfirmationPanel(anyString(), any(PCSCase.class), anyLong()))
            .thenReturn(confirmationPanelMarkup);

        return confirmationPanelMarkup;
    }
}
