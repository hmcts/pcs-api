package uk.gov.hmcts.reform.pcs.ccd.service.document;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.ccd.sdk.type.AddressUK;
import uk.gov.hmcts.ccd.sdk.type.DynamicList;
import uk.gov.hmcts.ccd.sdk.type.DynamicListElement;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.CaseFileCategory;
import uk.gov.hmcts.reform.pcs.ccd.domain.DocumentType;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.documentamend.DocumentAmendDetails;
import uk.gov.hmcts.reform.pcs.ccd.entity.DocumentEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.service.PcsCaseService;
import uk.gov.hmcts.reform.pcs.ccd.util.AddressFormatter;
import uk.gov.hmcts.reform.pcs.config.JacksonConfiguration;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.pcs.ccd.domain.CaseFileCategory.APPLICATIONS;
import static uk.gov.hmcts.reform.pcs.ccd.domain.CaseFileCategory.EVIDENCE;

@ExtendWith(MockitoExtension.class)
class DocumentSelectionServiceTest {

    private static final long CASE_REFERENCE = 1234567890123456L;

    @Mock
    private PcsCaseService pcsCaseService;

    private DocumentSelectionService underTest;

    @BeforeEach
    void setUp() {
        underTest = new DocumentSelectionService(pcsCaseService, new AddressFormatter());
    }

    @Test
    void shouldDefineFolderDropdownWithCaseFileViewFolders() {
        assertThat(CaseFileCategory.values())
            .extracting(CaseFileCategory::getLabel)
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
    void shouldPopulatePropertyAddressSummaryAndSharedDocumentLists() {
        DocumentEntity applicationDocument = document("application.pdf", APPLICATIONS.getId());
        when(pcsCaseService.loadCase(CASE_REFERENCE)).thenReturn(PcsCaseEntity.builder()
            .documents(List.of(applicationDocument))
            .build());
        PCSCase caseData = PCSCase.builder()
            .propertyAddress(AddressUK.builder()
                .addressLine1("15 Garden Drive")
                .postTown("Luton")
                .postCode("LU1 1AB")
                .build())
            .documentAmendDetails(DocumentAmendDetails.builder().build())
            .build();

        underTest.initialise(CASE_REFERENCE, caseData, caseData.getDocumentAmendDetails());

        assertThat(caseData.getDocumentAmendDetails().getPropertyAddressSummary())
            .isEqualTo("15 Garden Drive, Luton, LU1 1AB");
        assertThat(caseData.getApplicationsDocuments().getListItems())
            .extracting(DynamicListElement::getLabel)
            .containsExactly("application.pdf");
        assertThat(caseData.getDocumentAmendDetails().getApplicationsEmpty()).isEqualTo(YesOrNo.NO);
        assertThat(caseData.getDocumentAmendDetails().getEvidenceEmpty()).isEqualTo(YesOrNo.YES);
    }

    @Test
    void shouldSerialiseSharedDocumentListsWithoutDocumentAmendPrefix() throws JsonProcessingException {
        PCSCase caseData = PCSCase.builder()
            .applicationsDocuments(DynamicList.builder()
                .value(DynamicListElement.EMPTY)
                .listItems(List.of(DynamicListElement.builder()
                    .code(UUID.randomUUID())
                    .label("application.pdf")
                    .build()))
                .build())
            .documentAmendDetails(DocumentAmendDetails.builder()
                .applicationsEmpty(YesOrNo.NO)
                .build())
            .build();

        String serialisedCaseData = new JacksonConfiguration().getMapper().writeValueAsString(caseData);

        assertThat(serialisedCaseData).contains("\"applicationsDocuments\"");
        assertThat(serialisedCaseData).contains("\"documentAmend_ApplicationsEmpty\"");
        assertThat(serialisedCaseData).doesNotContain("documentAmend_ApplicationsDocuments");
    }

    @Test
    void shouldDeserialiseSelectedDocumentFromTopLevelDynamicListValue() throws JsonProcessingException {
        UUID documentId = UUID.fromString("aae85c47-84ca-4531-a5a8-ba170cfb8742");
        PCSCase caseData = new JacksonConfiguration().getMapper().readValue("""
            {
              "documentAmend_SelectedFolder": "APPLICATIONS",
              "applicationsDocuments": {
                "value": {
                  "code": "aae85c47-84ca-4531-a5a8-ba170cfb8742",
                  "label": "Local test application.pdf"
                },
                "list_items": [
                  {
                    "code": "aae85c47-84ca-4531-a5a8-ba170cfb8742",
                    "label": "Local test application.pdf"
                  }
                ]
              }
            }
            """, PCSCase.class);
        when(pcsCaseService.loadCase(CASE_REFERENCE)).thenReturn(PcsCaseEntity.builder()
            .documents(List.of(document("Local test application.pdf", APPLICATIONS.getId(), documentId)))
            .build());

        underTest.initialise(CASE_REFERENCE, caseData, caseData.getDocumentAmendDetails());
        List<String> errors = underTest.validateAndStoreSelection(caseData, caseData.getDocumentAmendDetails());

        assertThat(errors).isEmpty();
        assertThat(caseData.getDocumentAmendDetails().getSelectedDocumentId()).isEqualTo(documentId.toString());
        assertThat(caseData.getDocumentAmendDetails().getSelectedDocumentFileName())
            .isEqualTo("Local test application.pdf");
    }

    @Test
    void shouldReturnDifferentFolderErrorWhenSelectedFolderHasNoDocuments() {
        when(pcsCaseService.loadCase(CASE_REFERENCE)).thenReturn(PcsCaseEntity.builder().documents(List.of()).build());
        PCSCase caseData = PCSCase.builder()
            .documentAmendDetails(DocumentAmendDetails.builder()
                .selectedFolder(EVIDENCE)
                .selectedDocumentId(UUID.randomUUID().toString())
                .selectedDocumentFileName("old.pdf")
                .build())
            .build();

        underTest.initialise(CASE_REFERENCE, caseData, caseData.getDocumentAmendDetails());
        List<String> errors = underTest.validateAndStoreSelection(caseData, caseData.getDocumentAmendDetails());

        assertThat(errors).containsExactly(DocumentSelectionService.SELECT_DIFFERENT_FOLDER_ERROR);
        assertThat(caseData.getDocumentAmendDetails().getSelectedFolderId()).isEqualTo(EVIDENCE.getId());
        assertThat(caseData.getDocumentAmendDetails().getSelectedFolderLabel()).isEqualTo(EVIDENCE.getLabel());
        assertSelectedDocumentSelectionCleared(caseData.getDocumentAmendDetails());
    }

    @Test
    void shouldReturnNoErrorsWhenSelectionDetailsAreMissing() {
        List<String> errors = underTest.validateAndStoreSelection(PCSCase.builder().build(), null);

        assertThat(errors).isEmpty();
    }

    @Test
    void shouldReturnNoErrorsWhenSelectedFolderIsMissing() {
        DocumentAmendDetails details = DocumentAmendDetails.builder()
            .selectedDocumentId(UUID.randomUUID().toString())
            .selectedDocumentFileName("old.pdf")
            .build();

        List<String> errors = underTest.validateAndStoreSelection(
            PCSCase.builder().documentAmendDetails(details).build(),
            details
        );

        assertThat(errors).isEmpty();
        assertThat(details.getSelectedDocumentId()).isNotNull();
        assertThat(details.getSelectedDocumentFileName()).isEqualTo("old.pdf");
    }

    @Test
    void shouldClearSelectedDocumentWhenSelectedDynamicListValueIsBlank() {
        UUID documentId = UUID.randomUUID();
        PCSCase caseData = PCSCase.builder()
            .documentAmendDetails(DocumentAmendDetails.builder()
                .selectedFolder(EVIDENCE)
                .selectedDocumentId(UUID.randomUUID().toString())
                .selectedDocumentFileName("old.pdf")
                .build())
            .evidenceDocuments(DynamicList.builder()
                .value(DynamicListElement.EMPTY)
                .listItems(List.of(DynamicListElement.builder()
                    .code(documentId)
                    .label("evidence.pdf")
                    .build()))
                .build())
            .build();

        List<String> errors = underTest.validateAndStoreSelection(caseData, caseData.getDocumentAmendDetails());

        assertThat(errors).isEmpty();
        assertThat(caseData.getDocumentAmendDetails().getSelectedFolderId()).isEqualTo(EVIDENCE.getId());
        assertThat(caseData.getDocumentAmendDetails().getSelectedFolderLabel()).isEqualTo(EVIDENCE.getLabel());
        assertSelectedDocumentSelectionCleared(caseData.getDocumentAmendDetails());
    }

    @Test
    void shouldResolveSelectedDocumentByCodeWhenOnlyCodeIsPostedBack() {
        UUID documentId = UUID.randomUUID();
        PCSCase caseData = PCSCase.builder()
            .documentAmendDetails(DocumentAmendDetails.builder()
                .selectedFolder(EVIDENCE)
                .build())
            .evidenceDocuments(DynamicList.builder()
                .value(DynamicListElement.builder().code(documentId).build())
                .listItems(List.of(DynamicListElement.builder()
                    .code(documentId)
                    .label("evidence.pdf")
                    .build()))
                .build())
            .build();

        List<String> errors = underTest.validateAndStoreSelection(caseData, caseData.getDocumentAmendDetails());

        assertThat(errors).isEmpty();
        assertThat(caseData.getDocumentAmendDetails().getSelectedDocumentId()).isEqualTo(documentId.toString());
        assertThat(caseData.getDocumentAmendDetails().getSelectedDocumentFileName()).isEqualTo("evidence.pdf");
    }

    @Test
    void shouldResolveSelectedDocumentByLabelWhenOnlyLabelIsPostedBack() {
        UUID documentId = UUID.randomUUID();
        PCSCase caseData = PCSCase.builder()
            .documentAmendDetails(DocumentAmendDetails.builder()
                .selectedFolder(EVIDENCE)
                .build())
            .evidenceDocuments(DynamicList.builder()
                .value(DynamicListElement.builder().label("evidence.pdf").build())
                .listItems(List.of(DynamicListElement.builder()
                    .code(documentId)
                    .label("evidence.pdf")
                    .build()))
                .build())
            .build();

        List<String> errors = underTest.validateAndStoreSelection(caseData, caseData.getDocumentAmendDetails());

        assertThat(errors).isEmpty();
        assertThat(caseData.getDocumentAmendDetails().getSelectedDocumentId()).isEqualTo(documentId.toString());
        assertThat(caseData.getDocumentAmendDetails().getSelectedDocumentFileName()).isEqualTo("evidence.pdf");
    }

    @Test
    void shouldPersistSelectedFolderAndDocumentDetails() {
        UUID documentId = UUID.randomUUID();
        DocumentEntity document = document("photo.version.1.pdf", EVIDENCE.getId(), documentId);
        document.setSubmittedDate(Instant.parse("2026-03-01T10:15:30Z"));
        when(pcsCaseService.loadCase(CASE_REFERENCE)).thenReturn(PcsCaseEntity.builder()
            .documents(List.of(document))
            .build());
        PCSCase caseData = PCSCase.builder()
            .documentAmendDetails(DocumentAmendDetails.builder()
                .selectedFolder(EVIDENCE)
                .build())
            .evidenceDocuments(DynamicList.builder()
                .value(DynamicListElement.builder().code(documentId).build())
                .build())
            .build();

        underTest.initialise(CASE_REFERENCE, caseData, caseData.getDocumentAmendDetails());
        List<String> errors = underTest.validateAndStoreSelection(caseData, caseData.getDocumentAmendDetails());

        assertThat(errors).isEmpty();
        assertThat(caseData.getDocumentAmendDetails().getSelectedFolderId()).isEqualTo(EVIDENCE.getId());
        assertThat(caseData.getDocumentAmendDetails().getSelectedFolderLabel()).isEqualTo(EVIDENCE.getLabel());
        assertThat(caseData.getDocumentAmendDetails().getSelectedDocumentId()).isEqualTo(documentId.toString());
        assertThat(caseData.getDocumentAmendDetails().getSelectedDocumentFileName()).isEqualTo("photo.version.1.pdf");
    }

    @Test
    void shouldExcludeDefendantAccessCodeDocumentsFromSelection() {
        DocumentEntity accessCodeDocument = document("access code.pdf", EVIDENCE.getId());
        accessCodeDocument.setType(DocumentType.DEFENDANT_ACCESS_CODE);
        when(pcsCaseService.loadCase(CASE_REFERENCE)).thenReturn(PcsCaseEntity.builder()
            .documents(List.of(accessCodeDocument))
            .build());
        PCSCase caseData = PCSCase.builder()
            .documentAmendDetails(DocumentAmendDetails.builder().build())
            .build();

        underTest.initialise(CASE_REFERENCE, caseData, caseData.getDocumentAmendDetails());

        assertThat(caseData.getEvidenceDocuments().getListItems()).isEmpty();
        assertThat(caseData.getDocumentAmendDetails().getEvidenceEmpty()).isEqualTo(YesOrNo.YES);
    }

    private static DocumentEntity document(String fileName, String categoryId) {
        return document(fileName, categoryId, UUID.randomUUID());
    }

    private static DocumentEntity document(String fileName, String categoryId, UUID id) {
        return DocumentEntity.builder()
            .id(id)
            .fileName(fileName)
            .categoryId(categoryId)
            .build();
    }

    private static void assertSelectedDocumentSelectionCleared(DocumentAmendDetails details) {
        assertThat(details.getSelectedDocumentId()).isNull();
        assertThat(details.getSelectedDocumentFileName()).isNull();
    }
}
