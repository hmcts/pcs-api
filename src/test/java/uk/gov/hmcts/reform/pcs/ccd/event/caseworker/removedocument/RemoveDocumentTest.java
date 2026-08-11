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

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class RemoveDocumentTest extends BaseEventTest {

    @Mock
    private DocumentSelectionService documentSelectionService;
    @Mock
    private DocumentRemovalService documentRemovalService;
    @Mock
    private SelectDocumentToRemovePage selectDocumentToRemovePage;

    @BeforeEach
    void setUp() {
        RemoveDocument underTest = new RemoveDocument(
            documentSelectionService, documentRemovalService, selectDocumentToRemovePage
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
        UUID documentId = UUID.randomUUID();
        String reason = "No longer required";
        DocumentRemovalDetails details = DocumentRemovalDetails.builder()
            .selectedFolder(CaseFileCategory.EVIDENCE)
            .selectedDocumentId(documentId.toString())
            .selectedDocumentFileName("evidence.pdf")
            .propertyAddressSummary("123 Test Street, Testville, TE1 1ST")
            .evidenceReason(reason)
            .build();
        PCSCase caseData = PCSCase.builder()
            .documentRemovalDetails(details)
            .caseNameHmctsInternal("Smith v Jones")
            .build();

        SubmitResponse<State> response = callSubmitHandler(caseData);

        verify(documentRemovalService).removeDocument(documentId, reason);
        assertThat(response.getConfirmationBody())
            .contains("evidence.pdf")
            .contains("Case number: " + TEST_CASE_REFERENCE)
            .contains("123 Test Street, Testville, TE1 1ST")
            .contains("Smith v Jones");
    }
}
