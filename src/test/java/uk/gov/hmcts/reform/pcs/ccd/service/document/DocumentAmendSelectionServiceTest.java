package uk.gov.hmcts.reform.pcs.ccd.service.document;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.ccd.sdk.type.DynamicList;
import uk.gov.hmcts.ccd.sdk.type.DynamicListElement;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.CaseFileCategory;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.documentamend.DocumentAmendDetails;
import uk.gov.hmcts.reform.pcs.ccd.entity.DocumentEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.GenAppEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.service.PcsCaseService;
import uk.gov.hmcts.reform.pcs.ccd.type.DynamicStringList;
import uk.gov.hmcts.reform.pcs.ccd.type.DynamicStringListElement;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.pcs.ccd.domain.CaseFileCategory.APPLICATIONS;
import static uk.gov.hmcts.reform.pcs.ccd.domain.CaseFileCategory.EVIDENCE;
import static uk.gov.hmcts.reform.pcs.ccd.domain.CaseFileCategory.UNCATEGORISED_DOCUMENTS;

@ExtendWith(MockitoExtension.class)
class DocumentAmendSelectionServiceTest {

    private static final long CASE_REFERENCE = 1234567890123456L;

    @Mock
    private PcsCaseService pcsCaseService;

    private DocumentAmendSelectionService underTest;

    @BeforeEach
    void setUp() {
        underTest = new DocumentAmendSelectionService(pcsCaseService);
    }

    @Test
    void shouldPopulateFolderDropdownWithCaseFileViewFolders() {
        when(pcsCaseService.loadCase(CASE_REFERENCE)).thenReturn(PcsCaseEntity.builder().build());
        PCSCase caseData = PCSCase.builder().build();

        underTest.initialise(CASE_REFERENCE, caseData);

        assertThat(caseData.getDocumentAmendDetails().getSelectedFolder().getListItems())
            .extracting(DynamicListElement::getLabel)
            .containsExactly(
                "Statements of case",
                "Property documents",
                "Evidence",
                "Hearing documents",
                "Orders and Notice of Hearings",
                "Applications",
                "Appeals",
                "Correspondence",
                "Uncategorised documents"
            );
    }

    @Test
    void shouldPopulateDocumentsForSelectedCategoryAndExcludeWithoutNoticeApplicationDocuments() {
        DocumentEntity visibleEvidence = document("visible evidence.pdf", EVIDENCE.getId(), null);
        DocumentEntity hiddenApplicationDocument = document(
            "hidden application.pdf",
            APPLICATIONS.getId(),
            GenAppEntity.builder().withoutNotice(VerticalYesNo.YES).build()
        );
        DocumentEntity visibleApplicationDocument = document(
            "visible application.pdf",
            APPLICATIONS.getId(),
            GenAppEntity.builder().withoutNotice(VerticalYesNo.NO).build()
        );
        PcsCaseEntity pcsCase = PcsCaseEntity.builder()
            .documents(List.of(visibleEvidence, hiddenApplicationDocument, visibleApplicationDocument))
            .build();
        when(pcsCaseService.loadCase(CASE_REFERENCE)).thenReturn(pcsCase);
        PCSCase caseData = PCSCase.builder().build();

        underTest.initialise(CASE_REFERENCE, caseData);

        DocumentAmendDetails details = caseData.getDocumentAmendDetails();
        assertThat(details.getEvidenceDocuments().getListItems())
            .extracting(DynamicStringListElement::getLabel)
            .containsExactly("visible evidence.pdf");
        assertThat(details.getApplicationsDocuments().getListItems())
            .extracting(DynamicStringListElement::getLabel)
            .containsExactly("visible application.pdf");
    }

    @Test
    void shouldShowDocumentsWithNullCategoryUnderUncategorisedDocuments() {
        DocumentEntity uncategorisedDocument = document("loose document.pdf", null, null);
        when(pcsCaseService.loadCase(CASE_REFERENCE)).thenReturn(PcsCaseEntity.builder()
            .documents(List.of(uncategorisedDocument))
            .build());
        PCSCase caseData = PCSCase.builder().build();

        underTest.initialise(CASE_REFERENCE, caseData);

        assertThat(caseData.getDocumentAmendDetails().getUncategorisedDocuments().getListItems())
            .extracting(DynamicStringListElement::getLabel)
            .containsExactly("loose document.pdf");
        assertThat(caseData.getDocumentAmendDetails().getUncategorisedDocumentsEmpty()).isEqualTo(YesOrNo.NO);
    }

    @Test
    void shouldReturnDifferentFolderErrorWhenSelectedFolderHasNoDocuments() {
        when(pcsCaseService.loadCase(CASE_REFERENCE)).thenReturn(PcsCaseEntity.builder().build());
        PCSCase caseData = PCSCase.builder()
            .documentAmendDetails(DocumentAmendDetails.builder()
                .selectedFolder(selectedFolder(UNCATEGORISED_DOCUMENTS))
                .build())
            .build();
        underTest.initialise(CASE_REFERENCE, caseData);

        List<String> errors = underTest.validateAndStoreSelection(caseData);

        assertThat(errors).containsExactly("Select a different folder to continue");
        assertThat(caseData.getDocumentAmendDetails().getSelectedDocumentId()).isNull();
    }

    @Test
    void shouldPersistSelectedFolderAndDocumentDetails() {
        DocumentEntity document = document("photo.pdf", EVIDENCE.getId(), null);
        when(pcsCaseService.loadCase(CASE_REFERENCE)).thenReturn(PcsCaseEntity.builder()
            .documents(List.of(document))
            .build());
        PCSCase caseData = PCSCase.builder()
            .documentAmendDetails(DocumentAmendDetails.builder()
                .selectedFolder(selectedFolder(EVIDENCE))
                .evidenceDocuments(selectedDocument(document))
                .build())
            .build();
        underTest.initialise(CASE_REFERENCE, caseData);

        List<String> errors = underTest.validateAndStoreSelection(caseData);

        assertThat(errors).isEmpty();
        assertThat(caseData.getDocumentAmendDetails().getSelectedFolderId()).isEqualTo(EVIDENCE.getId());
        assertThat(caseData.getDocumentAmendDetails().getSelectedFolderLabel()).isEqualTo(EVIDENCE.getLabel());
        assertThat(caseData.getDocumentAmendDetails().getSelectedDocumentId()).isEqualTo(document.getId().toString());
        assertThat(caseData.getDocumentAmendDetails().getSelectedDocumentFileName()).isEqualTo("photo.pdf");
    }

    @Test
    void shouldLeaveMissingDocumentSelectionToExuiMandatoryValidation() {
        DocumentEntity document = document("photo.pdf", EVIDENCE.getId(), null);
        when(pcsCaseService.loadCase(CASE_REFERENCE)).thenReturn(PcsCaseEntity.builder()
            .documents(List.of(document))
            .build());
        PCSCase caseData = PCSCase.builder()
            .documentAmendDetails(DocumentAmendDetails.builder()
                .selectedFolder(selectedFolder(EVIDENCE))
                .build())
            .build();
        underTest.initialise(CASE_REFERENCE, caseData);

        List<String> errors = underTest.validateAndStoreSelection(caseData);

        assertThat(errors).isEmpty();
        assertThat(caseData.getDocumentAmendDetails().getSelectedDocumentId()).isNull();
    }

    private static DynamicList selectedFolder(CaseFileCategory category) {
        DynamicListElement selectedFolder = new DynamicListElement(
            DocumentAmendSelectionService.folderCode(category),
            category.getLabel()
        );
        return new DynamicList(selectedFolder, List.of(selectedFolder));
    }

    private static DynamicStringList selectedDocument(DocumentEntity document) {
        DynamicStringListElement selectedDocument = DynamicStringListElement.builder()
            .code(document.getId().toString())
            .label(document.getFileName())
            .build();

        return DynamicStringList.builder()
            .value(selectedDocument)
            .listItems(List.of(selectedDocument))
            .build();
    }

    private static DocumentEntity document(String fileName, String categoryId, GenAppEntity generalApplication) {
        return DocumentEntity.builder()
            .id(UUID.randomUUID())
            .fileName(fileName)
            .categoryId(categoryId)
            .generalApplication(generalApplication)
            .submittedDate(Instant.now())
            .build();
    }
}
