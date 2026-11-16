package uk.gov.hmcts.reform.pcs.ccd.event.caseworker.updatedocument;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.ccd.sdk.api.Permission;
import uk.gov.hmcts.ccd.sdk.api.callback.SubmitResponse;
import uk.gov.hmcts.ccd.sdk.type.Document;
import uk.gov.hmcts.ccd.sdk.type.ListValue;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.event.BaseEventTest;
import uk.gov.hmcts.reform.pcs.ccd.service.document.DocumentCategoryUpdateService;
import uk.gov.hmcts.reform.pcs.ccd.service.document.DocumentCategoryUpdateService.DocumentMove;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.pcs.ccd.accesscontrol.DocumentMoveRoles.DOCUMENT_MOVE_ROLES;

@ExtendWith(MockitoExtension.class)
class DocumentUpdatedTest extends BaseEventTest {

    @Mock
    private DocumentCategoryUpdateService documentCategoryUpdateService;

    @BeforeEach
    void setUp() {
        DocumentUpdated underTest = new DocumentUpdated(documentCategoryUpdateService);

        setEventUnderTest(underTest);
    }

    @Test
    void shouldRegisterSystemEventWithOnlyInternalAccess() {
        assertThat(configuredEvent.getId()).isEqualTo("DocumentUpdated");
        assertThat(getEventFieldIds()).isEmpty();
        assertThat(configuredEvent.getGrants().keySet()).containsExactlyInAnyOrderElementsOf(DOCUMENT_MOVE_ROLES);
        assertThat(configuredEvent.getGrants().values()).containsOnly(Permission.R);
    }

    @Test
    void shouldPassCcdDocumentToPersist() {
        var documents = List.of(ListValue.<Document>builder()
                .id("document-1").value(Document.builder().categoryId("evidence").build()).build());

        PCSCase caseData = PCSCase.builder().allDocuments(documents).build();
        callSubmitHandler(caseData);

        verify(documentCategoryUpdateService).updateCategories(TEST_CASE_REFERENCE, caseData);
    }

    @Test
    void shouldDescribeMoveInHistory() {
        PCSCase caseData = PCSCase.builder().build();
        when(documentCategoryUpdateService.updateCategories(TEST_CASE_REFERENCE, caseData))
            .thenReturn(List.of(new DocumentMove("notice.pdf", "uncategorisedDocuments", "evidence")));

        SubmitResponse<State> response = callSubmitHandler(caseData);

        assertThat(response.getEventMetadata().getSummary()).isEqualTo("Document moved to Evidence");
        assertThat(response.getEventMetadata().getDescription())
            .isEqualTo("notice.pdf: Uncategorised documents → Evidence");
    }

    @Test
    void shouldListEveryMoveWhenSeveralDocumentsMove() {
        PCSCase caseData = PCSCase.builder().build();
        when(documentCategoryUpdateService.updateCategories(TEST_CASE_REFERENCE, caseData))
            .thenReturn(List.of(new DocumentMove("a.pdf", "evidence", "appeals"),
                                new DocumentMove("b.pdf", "propertyDocuments", "unknownFolder")));

        SubmitResponse<State> response = callSubmitHandler(caseData);

        assertThat(response.getEventMetadata().getSummary()).isEqualTo("2 documents moved");
        assertThat(response.getEventMetadata().getDescription())
            .isEqualTo("a.pdf: Evidence → Appeals\nb.pdf: Property documents → unknownFolder");
    }

    @Test
    void shouldAddNoHistoryDetailWhenNothingMoved() {
        PCSCase caseData = PCSCase.builder().build();
        when(documentCategoryUpdateService.updateCategories(TEST_CASE_REFERENCE, caseData)).thenReturn(List.of());

        SubmitResponse<State> response = callSubmitHandler(caseData);

        assertThat(response.getEventMetadata()).isNull();
    }
}
