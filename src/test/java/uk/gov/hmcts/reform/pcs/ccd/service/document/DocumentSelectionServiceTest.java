package uk.gov.hmcts.reform.pcs.ccd.service.document;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.documentamend.DocumentAmendDetails;
import uk.gov.hmcts.reform.pcs.ccd.entity.DocumentEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.GenAppEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.service.PcsCaseService;
import uk.gov.hmcts.reform.pcs.ccd.util.AddressFormatter;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.pcs.ccd.domain.CaseFileCategory.APPLICATIONS;
import static uk.gov.hmcts.reform.pcs.ccd.domain.CaseFileCategory.EVIDENCE;
import static uk.gov.hmcts.reform.pcs.ccd.domain.CaseFileCategory.UNCATEGORISED_DOCUMENTS;

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
    void shouldPopulatePropertyAddressSummary() {
        when(pcsCaseService.loadCase(CASE_REFERENCE)).thenReturn(PcsCaseEntity.builder().build());
        PCSCase caseData = PCSCase.builder()
            .propertyAddress(AddressUK.builder()
                .addressLine1("15 Garden Drive")
                .postTown("Luton")
                .postCode("LU1 1AB")
                .build())
            .build();
        DocumentAmendDetails details = new DocumentAmendDetails();

        underTest.initialise(CASE_REFERENCE, caseData, details);

        assertThat(details.getPropertyAddressSummary())
            .isEqualTo("15 Garden Drive, Luton, LU1 1AB");
    }

    @Test
    void shouldSerialiseDocumentAmendFieldsWithGeneratedCcdFieldIds() throws JsonProcessingException {
        DynamicList applicationsDocuments = DynamicList.builder()
            .value(DynamicListElement.EMPTY)
            .listItems(List.of(DynamicListElement.builder()
                .code(UUID.randomUUID())
                .label("application.pdf")
                .build()))
            .build();
        PCSCase caseData = PCSCase.builder()
            .applicationsDocuments(applicationsDocuments)
            .documentAmendDetails(DocumentAmendDetails.builder()
                .applicationsEmpty(YesOrNo.NO)
                .build())
            .build();

        String serialisedCaseData = new ObjectMapper().writeValueAsString(caseData);

        assertThat(serialisedCaseData).contains("\"applicationsDocuments\"");
        assertThat(serialisedCaseData).contains("\"documentAmend_ApplicationsEmpty\"");
        assertThat(serialisedCaseData).doesNotContain("documentAmend_applicationsDocuments");
    }

    @Test
    void shouldPopulateDocumentsForSelectedCategoryAndIncludeWithoutNoticeApplicationDocuments() {
        DocumentEntity visibleEvidence = document("visible evidence.pdf", EVIDENCE.getId(), null);
        DocumentEntity withoutNoticeApplicationDocument = document(
            "without notice application.pdf",
            APPLICATIONS.getId(),
            GenAppEntity.builder().withoutNotice(VerticalYesNo.YES).build()
        );
        DocumentEntity visibleApplicationDocument = document(
            "visible application.pdf",
            APPLICATIONS.getId(),
            GenAppEntity.builder().withoutNotice(VerticalYesNo.NO).build()
        );
        PcsCaseEntity pcsCase = PcsCaseEntity.builder()
            .documents(List.of(visibleEvidence, withoutNoticeApplicationDocument, visibleApplicationDocument))
            .build();
        when(pcsCaseService.loadCase(CASE_REFERENCE)).thenReturn(pcsCase);
        PCSCase caseData = PCSCase.builder().build();
        DocumentAmendDetails details = new DocumentAmendDetails();

        underTest.initialise(CASE_REFERENCE, caseData, details);

        assertThat(caseData.getEvidenceDocuments().getListItems())
            .extracting(DynamicListElement::getLabel)
            .containsExactly("visible evidence.pdf");
        assertThat(caseData.getApplicationsDocuments().getListItems())
            .extracting(DynamicListElement::getLabel)
            .containsExactly("visible application.pdf", "without notice application.pdf");
        assertThat(caseData.getApplicationsDocuments().getValue()).isNull();
    }

    @Test
    void shouldExcludeDocumentsWithNullCategoryIdFromUncategorisedDocuments() {
        DocumentEntity nullCategoryDocument = document("loose document.pdf", null, null);
        DocumentEntity categorisedDocument = document(
            "uncategorised document.pdf",
            UNCATEGORISED_DOCUMENTS.getId(),
            null
        );
        when(pcsCaseService.loadCase(CASE_REFERENCE)).thenReturn(PcsCaseEntity.builder()
            .documents(List.of(nullCategoryDocument, categorisedDocument))
            .build());
        PCSCase caseData = PCSCase.builder().build();
        DocumentAmendDetails details = new DocumentAmendDetails();

        underTest.initialise(CASE_REFERENCE, caseData, details);

        assertThat(caseData.getUncategorisedDocuments().getListItems())
            .extracting(DynamicListElement::getLabel)
            .containsExactly("uncategorised document.pdf");
        assertThat(details.getUncategorisedDocumentsEmpty()).isEqualTo(YesOrNo.NO);
    }

    @Test
    void shouldExcludeDefendantAccessCodeDocumentsFromUncategorisedDocuments() {
        DocumentEntity accessCodeDocument = buildDocumentEntity(
            null,
            UNCATEGORISED_DOCUMENTS.getId(),
            DocumentType.DEFENDANT_ACCESS_CODE,
            false
        );
        DocumentEntity visibleDocument = buildDocumentEntity(
            "uncategorised document.pdf",
            UNCATEGORISED_DOCUMENTS.getId(),
            DocumentType.OTHER,
            false
        );
        when(pcsCaseService.loadCase(CASE_REFERENCE)).thenReturn(PcsCaseEntity.builder()
            .documents(List.of(accessCodeDocument, visibleDocument))
            .build());
        PCSCase caseData = PCSCase.builder().build();
        DocumentAmendDetails details = new DocumentAmendDetails();

        underTest.initialise(CASE_REFERENCE, caseData, details);

        assertThat(caseData.getUncategorisedDocuments().getListItems())
            .extracting(DynamicListElement::getLabel)
            .containsExactly("uncategorised document.pdf");
        assertThat(details.getUncategorisedDocumentsEmpty()).isEqualTo(YesOrNo.NO);
    }

    @Test
    void shouldExcludeRemovedDocumentsFromSelection() {
        DocumentEntity removedDocument = buildDocumentEntity("removed evidence.pdf", EVIDENCE.getId(), null, true);
        DocumentEntity activeDocument = buildDocumentEntity("active evidence.pdf", EVIDENCE.getId(), null, false);
        when(pcsCaseService.loadCase(CASE_REFERENCE)).thenReturn(PcsCaseEntity.builder()
            .documents(List.of(removedDocument, activeDocument))
            .build());
        PCSCase caseData = PCSCase.builder().build();
        DocumentAmendDetails details = new DocumentAmendDetails();

        underTest.initialise(CASE_REFERENCE, caseData, details);

        assertThat(caseData.getEvidenceDocuments().getListItems())
            .extracting(DynamicListElement::getLabel)
            .containsExactly("active evidence.pdf");
    }

    @Test
    void shouldOrderDocumentsBySubmittedDateDescendingThenFileNameWithNullDatesLast() {
        DocumentEntity older = document(
            "b older evidence.pdf",
            EVIDENCE.getId(),
            null,
            Instant.parse("2026-01-01T10:00:00Z")
        );
        DocumentEntity newerA = document(
            "a newer evidence.pdf",
            EVIDENCE.getId(),
            null,
            Instant.parse("2026-01-02T10:00:00Z")
        );
        DocumentEntity newerB = document(
            "b newer evidence.pdf",
            EVIDENCE.getId(),
            null,
            Instant.parse("2026-01-02T10:00:00Z")
        );
        DocumentEntity nullDate = document("null date evidence.pdf", EVIDENCE.getId(), null, null);
        when(pcsCaseService.loadCase(CASE_REFERENCE)).thenReturn(PcsCaseEntity.builder()
            .documents(List.of(older, nullDate, newerB, newerA))
            .build());
        PCSCase caseData = PCSCase.builder().build();
        DocumentAmendDetails details = new DocumentAmendDetails();

        underTest.initialise(CASE_REFERENCE, caseData, details);

        assertThat(caseData.getEvidenceDocuments().getListItems())
            .extracting(DynamicListElement::getLabel)
            .containsExactly(
                "a newer evidence.pdf",
                "b newer evidence.pdf",
                "b older evidence.pdf",
                "null date evidence.pdf"
            );
    }

    @Test
    void shouldReturnNoErrorsWhenDocumentAmendDetailsIsNull() {
        List<String> errors = underTest.validateAndStoreSelection(PCSCase.builder().build(),null);

        assertThat(errors).isEmpty();
    }

    @Test
    void shouldReturnDifferentFolderErrorWhenSelectedFolderHasNoDocuments() {
        when(pcsCaseService.loadCase(CASE_REFERENCE)).thenReturn(PcsCaseEntity.builder().build());
        PCSCase caseData = PCSCase.builder().build();
        DocumentAmendDetails details = DocumentAmendDetails.builder()
            .selectedFolder(UNCATEGORISED_DOCUMENTS)
            .build();
        underTest.initialise(CASE_REFERENCE, caseData, details);

        List<String> errors = underTest.validateAndStoreSelection(caseData, details);

        assertThat(errors).containsExactly("Select a different folder to continue");
        assertThat(details.getSelectedFolderId()).isEqualTo(UNCATEGORISED_DOCUMENTS.getId());
        assertThat(details.getSelectedFolderLabel()).isEqualTo(UNCATEGORISED_DOCUMENTS.getLabel());
        assertThat(details.getSelectedDocumentId()).isNull();
        assertThat(details.getSelectedDocumentFileName()).isNull();
    }

    @Test
    void shouldTreatEmptyDocumentSelectionAsNoSelection() {
        DocumentEntity document = document("photo.pdf", EVIDENCE.getId(), null);
        when(pcsCaseService.loadCase(CASE_REFERENCE)).thenReturn(PcsCaseEntity.builder()
            .documents(List.of(document))
            .build());
        PCSCase caseData = PCSCase.builder()
            .evidenceDocuments(DynamicList.builder().build())
            .build();
        DocumentAmendDetails details = DocumentAmendDetails.builder()
            .selectedFolder(EVIDENCE)
            .build();
        underTest.initialise(CASE_REFERENCE, caseData, details);

        List<String> errors = underTest.validateAndStoreSelection(caseData, details);

        assertThat(errors).isEmpty();
        assertThat(details.getSelectedDocumentId()).isNull();
        assertThat(details.getSelectedDocumentFileName()).isNull();
    }

    @Test
    void shouldPersistSelectedFolderAndDocumentDetails() {
        DocumentEntity document = document("photo.pdf", EVIDENCE.getId(), null);
        when(pcsCaseService.loadCase(CASE_REFERENCE)).thenReturn(PcsCaseEntity.builder()
            .documents(List.of(document))
            .build());
        PCSCase caseData = PCSCase.builder()
            .evidenceDocuments(selectedDocument(document))
            .build();
        DocumentAmendDetails details = DocumentAmendDetails.builder()
            .selectedFolder(EVIDENCE)
            .build();
        underTest.initialise(CASE_REFERENCE, caseData, details);

        List<String> errors = underTest.validateAndStoreSelection(caseData, details);

        assertThat(errors).isEmpty();
        assertThat(details.getSelectedFolderId()).isEqualTo(EVIDENCE.getId());
        assertThat(details.getSelectedFolderLabel()).isEqualTo(EVIDENCE.getLabel());
        assertThat(details.getSelectedDocumentId()).isEqualTo(document.getId().toString());
        assertThat(details.getSelectedDocumentFileName()).isEqualTo("photo.pdf");
    }

    @Test
    void shouldLeaveMissingDocumentSelectionToExuiMandatoryValidation() {
        DocumentEntity document = document("photo.pdf", EVIDENCE.getId(), null);
        when(pcsCaseService.loadCase(CASE_REFERENCE)).thenReturn(PcsCaseEntity.builder()
            .documents(List.of(document))
            .build());
        PCSCase caseData = PCSCase.builder().build();
        DocumentAmendDetails details = DocumentAmendDetails.builder()
            .selectedFolder(EVIDENCE)
            .build();
        underTest.initialise(CASE_REFERENCE, caseData, details);

        List<String> errors = underTest.validateAndStoreSelection(caseData, details);

        assertThat(errors).isEmpty();
        assertThat(details.getSelectedDocumentId()).isNull();
    }

    private static DynamicList selectedDocument(DocumentEntity document) {
        DynamicListElement selectedDocument = DynamicListElement.builder()
            .code(document.getId())
            .label(document.getFileName())
            .build();

        return DynamicList.builder()
            .value(selectedDocument)
            .listItems(List.of(selectedDocument))
            .build();
    }

    private static DocumentEntity document(String fileName, String categoryId, GenAppEntity generalApplication) {
        return document(fileName, categoryId, generalApplication, Instant.now());
    }

    private static DocumentEntity document(String fileName, String categoryId, GenAppEntity generalApplication,
                                           Instant submittedDate) {
        return DocumentEntity.builder()
            .id(UUID.randomUUID())
            .fileName(fileName)
            .categoryId(categoryId)
            .generalApplication(generalApplication)
            .submittedDate(submittedDate)
            .build();
    }

    private static DocumentEntity buildDocumentEntity(String fileName, String categoryId, DocumentType type,
                                                       boolean removed) {
        return DocumentEntity.builder()
            .id(UUID.randomUUID())
            .fileName(fileName)
            .categoryId(categoryId)
            .type(type)
            .removed(removed)
            .submittedDate(Instant.now())
            .build();
    }
}
