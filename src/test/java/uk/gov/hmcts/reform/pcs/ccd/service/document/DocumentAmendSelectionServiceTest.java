package uk.gov.hmcts.reform.pcs.ccd.service.document;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.CaseFileCategory;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.Party;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.documentamend.DocumentAmendDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.documentamend.DocumentAmendFolder;
import uk.gov.hmcts.reform.pcs.ccd.entity.DocumentEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.GenAppEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.service.CaseNameFormatter;
import uk.gov.hmcts.reform.pcs.ccd.service.PcsCaseService;
import uk.gov.hmcts.reform.pcs.ccd.type.DynamicStringList;
import uk.gov.hmcts.reform.pcs.ccd.type.DynamicStringListElement;
import uk.gov.hmcts.reform.pcs.ccd.util.AddressFormatter;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.pcs.ccd.domain.CaseFileCategory.APPLICATIONS;
import static uk.gov.hmcts.reform.pcs.ccd.domain.CaseFileCategory.EVIDENCE;
import static uk.gov.hmcts.reform.pcs.ccd.domain.CaseFileCategory.UNCATEGORISED_DOCUMENTS;
import static uk.gov.hmcts.reform.pcs.ccd.util.ListValueUtils.wrapListItems;

@ExtendWith(MockitoExtension.class)
class DocumentAmendSelectionServiceTest {

    private static final long CASE_REFERENCE = 1234567890123456L;

    @Mock
    private PcsCaseService pcsCaseService;

    private DocumentAmendSelectionService underTest;

    @BeforeEach
    void setUp() {
        underTest = new DocumentAmendSelectionService(pcsCaseService, new AddressFormatter(), new CaseNameFormatter());
    }

    @Test
    void shouldDefineFolderDropdownWithCaseFileViewFolders() {
        assertThat(DocumentAmendFolder.values())
            .extracting(DocumentAmendFolder::getLabel)
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
    void shouldPopulateCompactCaseSummaryFields() {
        when(pcsCaseService.loadCase(CASE_REFERENCE)).thenReturn(PcsCaseEntity.builder().build());
        PCSCase caseData = PCSCase.builder()
            .propertyAddress(uk.gov.hmcts.ccd.sdk.type.AddressUK.builder()
                .addressLine1("15 Garden Drive")
                .postTown("Luton")
                .postCode("LU1 1AB")
                .build())
            .claimantNames("Treetops Housing")
            .defendantNames("Billy Wright")
            .build();

        underTest.initialise(CASE_REFERENCE, caseData);

        assertThat(caseData.getDocumentAmendDetails().getPropertyAddressSummary())
            .isEqualTo("15 Garden Drive, Luton, LU1 1AB");
        assertThat(caseData.getDocumentAmendDetails().getPartyNamesSummary())
            .isEqualTo("Treetops Housing vs Billy Wright");
    }

    @Test
    void shouldPopulatePartyNamesSummaryFromPartyCollectionsWhenSummaryFieldsAreMissing() {
        when(pcsCaseService.loadCase(CASE_REFERENCE)).thenReturn(PcsCaseEntity.builder().build());
        PCSCase caseData = PCSCase.builder()
            .allClaimants(wrapListItems(List.of(Party.builder()
                .orgName("Treetops Housing")
                .build())))
            .allDefendants(wrapListItems(List.of(Party.builder()
                .firstName("Billy")
                .lastName("Wright")
                .nameKnown(VerticalYesNo.YES)
                .build())))
            .build();

        underTest.initialise(CASE_REFERENCE, caseData);

        assertThat(caseData.getDocumentAmendDetails().getPartyNamesSummary())
            .isEqualTo("Treetops Housing vs Billy Wright");
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
            .extracting(DynamicStringListElement::getLabel)
            .containsExactly("visible evidence.pdf");
        assertThat(details.getApplicationsDocuments().getListItems())
            .extracting(DynamicStringListElement::getLabel)
            .containsExactly("visible application.pdf", "without notice application.pdf");
    }

    @Test
    void shouldOnlyShowDocumentsWithUncategorisedCategoryUnderUncategorisedDocuments() {
        DocumentEntity uncategorisedDocument = document("loose document.pdf", null, null);
        DocumentEntity categorisedDocument = document(
            "uncategorised document.pdf",
            UNCATEGORISED_DOCUMENTS.getId(),
            null
        );
        when(pcsCaseService.loadCase(CASE_REFERENCE)).thenReturn(PcsCaseEntity.builder()
            .documents(List.of(uncategorisedDocument, categorisedDocument))
            .build());
        PCSCase caseData = PCSCase.builder().build();

        underTest.initialise(CASE_REFERENCE, caseData);

        assertThat(caseData.getDocumentAmendDetails().getUncategorisedDocuments().getListItems())
            .extracting(DynamicStringListElement::getLabel)
            .containsExactly("uncategorised document.pdf");
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
    void shouldReturnErrorWhenSelectedFolderHasDocumentsButNoDocumentSelected() {
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

        assertThat(errors).containsExactly("Select which document you want to amend");
        assertThat(caseData.getDocumentAmendDetails().getSelectedDocumentId()).isNull();
    }

    private static DocumentAmendFolder selectedFolder(CaseFileCategory category) {
        return switch (category) {
            case STATEMENTS_OF_CASE -> DocumentAmendFolder.STATEMENTS_OF_CASE;
            case PROPERTY_DOCUMENTS -> DocumentAmendFolder.PROPERTY_DOCUMENTS;
            case EVIDENCE -> DocumentAmendFolder.EVIDENCE;
            case HEARING_DOCUMENTS -> DocumentAmendFolder.HEARING_DOCUMENTS;
            case ORDERS_AND_NOTICE_OF_HEARINGS -> DocumentAmendFolder.ORDERS_AND_NOTICE_OF_HEARINGS;
            case APPLICATIONS -> DocumentAmendFolder.APPLICATIONS;
            case APPEALS -> DocumentAmendFolder.APPEALS;
            case CORRESPONDENCE -> DocumentAmendFolder.CORRESPONDENCE;
            case UNCATEGORISED_DOCUMENTS -> DocumentAmendFolder.UNCATEGORISED_DOCUMENTS;
        };
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
