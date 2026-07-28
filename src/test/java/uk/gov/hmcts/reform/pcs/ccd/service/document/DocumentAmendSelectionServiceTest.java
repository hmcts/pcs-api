package uk.gov.hmcts.reform.pcs.ccd.service.document;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
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
class DocumentAmendSelectionServiceTest {

    private static final long CASE_REFERENCE = 1234567890123456L;

    @Mock
    private PcsCaseService pcsCaseService;

    private DocumentAmendSelectionService underTest;

    @BeforeEach
    void setUp() {
        underTest = new DocumentAmendSelectionService(pcsCaseService, new AddressFormatter());
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
            .propertyAddress(uk.gov.hmcts.ccd.sdk.type.AddressUK.builder()
                .addressLine1("15 Garden Drive")
                .postTown("Luton")
                .postCode("LU1 1AB")
                .build())
            .build();

        underTest.initialise(CASE_REFERENCE, caseData);

        assertThat(caseData.getDocumentAmendDetails().getPropertyAddressSummary())
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
            .documentAmendDetails(DocumentAmendDetails.builder()
                .applicationsDocuments(applicationsDocuments)
                .applicationsEmpty(YesOrNo.NO)
                .build())
            .build();

        String serialisedCaseData = new ObjectMapper().writeValueAsString(caseData);

        assertThat(serialisedCaseData).contains("\"documentAmend_ApplicationsDocuments\"");
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

        underTest.initialise(CASE_REFERENCE, caseData);

        DocumentAmendDetails details = caseData.getDocumentAmendDetails();
        assertThat(details.getEvidenceDocuments().getListItems())
            .extracting(DynamicListElement::getLabel)
            .containsExactly("visible evidence.pdf");
        assertThat(details.getApplicationsDocuments().getListItems())
            .extracting(DynamicListElement::getLabel)
            .containsExactly("visible application.pdf", "without notice application.pdf");
        assertThat(details.getApplicationsDocuments().getValue()).isNull();
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

        underTest.initialise(CASE_REFERENCE, caseData);

        assertThat(caseData.getDocumentAmendDetails().getUncategorisedDocuments().getListItems())
            .extracting(DynamicListElement::getLabel)
            .containsExactly("uncategorised document.pdf");
        assertThat(caseData.getDocumentAmendDetails().getUncategorisedDocumentsEmpty()).isEqualTo(YesOrNo.NO);
    }

    @Test
    void shouldExcludeDefendantAccessCodeDocumentsFromUncategorisedDocuments() {
        DocumentEntity accessCodeDocument = documentWithType(
            null,
            UNCATEGORISED_DOCUMENTS.getId(),
            DocumentType.DEFENDANT_ACCESS_CODE
        );
        DocumentEntity visibleDocument = documentWithType(
            "uncategorised document.pdf",
            UNCATEGORISED_DOCUMENTS.getId(),
            DocumentType.OTHER
        );
        when(pcsCaseService.loadCase(CASE_REFERENCE)).thenReturn(PcsCaseEntity.builder()
            .documents(List.of(accessCodeDocument, visibleDocument))
            .build());
        PCSCase caseData = PCSCase.builder().build();

        underTest.initialise(CASE_REFERENCE, caseData);

        assertThat(caseData.getDocumentAmendDetails().getUncategorisedDocuments().getListItems())
            .extracting(DynamicListElement::getLabel)
            .containsExactly("uncategorised document.pdf");
        assertThat(caseData.getDocumentAmendDetails().getUncategorisedDocumentsEmpty()).isEqualTo(YesOrNo.NO);
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

        underTest.initialise(CASE_REFERENCE, caseData);

        assertThat(caseData.getDocumentAmendDetails().getEvidenceDocuments().getListItems())
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
        List<String> errors = underTest.validateAndStoreSelection(PCSCase.builder().build());

        assertThat(errors).isEmpty();
    }

    @Test
    void shouldReturnDifferentFolderErrorWhenSelectedFolderHasNoDocuments() {
        when(pcsCaseService.loadCase(CASE_REFERENCE)).thenReturn(PcsCaseEntity.builder().build());
        PCSCase caseData = PCSCase.builder()
            .documentAmendDetails(DocumentAmendDetails.builder()
                .selectedFolder(UNCATEGORISED_DOCUMENTS)
                .build())
            .build();
        underTest.initialise(CASE_REFERENCE, caseData);

        List<String> errors = underTest.validateAndStoreSelection(caseData);

        assertThat(errors).containsExactly("Select a different folder to continue");
        assertThat(caseData.getDocumentAmendDetails().getSelectedFolderId())
            .isEqualTo(UNCATEGORISED_DOCUMENTS.getId());
        assertThat(caseData.getDocumentAmendDetails().getSelectedFolderLabel())
            .isEqualTo(UNCATEGORISED_DOCUMENTS.getLabel());
        assertThat(caseData.getDocumentAmendDetails().getSelectedDocumentId()).isNull();
        assertThat(caseData.getDocumentAmendDetails().getSelectedDocumentFileName()).isNull();
    }

    @Test
    void shouldTreatEmptyDocumentSelectionAsNoSelection() {
        DocumentEntity document = document("photo.pdf", EVIDENCE.getId(), null);
        when(pcsCaseService.loadCase(CASE_REFERENCE)).thenReturn(PcsCaseEntity.builder()
            .documents(List.of(document))
            .build());
        PCSCase caseData = PCSCase.builder()
            .documentAmendDetails(DocumentAmendDetails.builder()
                .selectedFolder(EVIDENCE)
                .evidenceDocuments(DynamicList.builder()
                    .build())
                .build())
            .build();
        underTest.initialise(CASE_REFERENCE, caseData);

        List<String> errors = underTest.validateAndStoreSelection(caseData);

        assertThat(errors).isEmpty();
        assertThat(caseData.getDocumentAmendDetails().getSelectedDocumentId()).isNull();
        assertThat(caseData.getDocumentAmendDetails().getSelectedDocumentFileName()).isNull();
    }

    @Test
    void shouldPersistSelectedFolderAndDocumentDetails() {
        DocumentEntity document = document("photo.pdf", EVIDENCE.getId(), null);
        when(pcsCaseService.loadCase(CASE_REFERENCE)).thenReturn(PcsCaseEntity.builder()
            .documents(List.of(document))
            .build());
        PCSCase caseData = PCSCase.builder()
            .documentAmendDetails(DocumentAmendDetails.builder()
                .selectedFolder(EVIDENCE)
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
                .selectedFolder(EVIDENCE)
                .build())
            .build();
        underTest.initialise(CASE_REFERENCE, caseData);

        List<String> errors = underTest.validateAndStoreSelection(caseData);

        assertThat(errors).isEmpty();
        assertThat(caseData.getDocumentAmendDetails().getSelectedDocumentId()).isNull();
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

    private static DocumentEntity documentWithType(String fileName, String categoryId, DocumentType type) {
        return DocumentEntity.builder()
            .id(UUID.randomUUID())
            .fileName(fileName)
            .categoryId(categoryId)
            .type(type)
            .submittedDate(Instant.now())
            .build();
    }
}
