package uk.gov.hmcts.reform.pcs.ccd.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.ccd.sdk.type.Document;
import uk.gov.hmcts.ccd.sdk.type.ListValue;
import uk.gov.hmcts.reform.pcs.ccd.domain.AdditionalDocument;
import uk.gov.hmcts.reform.pcs.ccd.domain.AdditionalDocumentType;
import uk.gov.hmcts.reform.pcs.ccd.domain.CaseFileCategory;
import uk.gov.hmcts.reform.pcs.ccd.domain.DocumentType;
import uk.gov.hmcts.reform.pcs.ccd.domain.NoticeServedDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.RentArrearsSection;
import uk.gov.hmcts.reform.pcs.ccd.domain.TenancyLicenceDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.EnforcementOrder;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.warrantofrestitution.EvidenceDocumentType;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.warrantofrestitution.EvidenceOfDefendants;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.warrantofrestitution.WarrantOfRestitutionDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.wales.OccupationLicenceDetailsWales;
import uk.gov.hmcts.reform.pcs.ccd.entity.DocumentEntity;
import uk.gov.hmcts.reform.pcs.ccd.repository.DocumentRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DocumentServiceTest {

    @Mock
    private DocumentRepository documentRepository;

    @Captor
    private ArgumentCaptor<List<DocumentEntity>> documentEntityListCaptor;

    private DocumentService underTest;

    @BeforeEach
    void setUp() {
        underTest = new DocumentService(documentRepository);
    }

    @Test
    void shouldSaveTwoAdditionalDocumentTypes() {
        // Given
        PCSCase pcsCase = mock(PCSCase.class);

        AdditionalDocument additionalDocument1 = AdditionalDocument.builder()
            .document(Document.builder()
                          .url("url-WITNESS_STATEMENT")
                          .filename("file-WITNESS_STATEMENT")
                          .binaryUrl("bin-WITNESS_STATEMENT")
                          .categoryId("cat-WITNESS_STATEMENT")
                          .build())
            .documentType(AdditionalDocumentType.WITNESS_STATEMENT)
            .build();

        AdditionalDocument additionalDocument2 = AdditionalDocument.builder()
            .document(Document.builder()
                           .url("url-RENT_STATEMENT")
                           .filename("file-RENT_STATEMENT")
                           .binaryUrl("bin-RENT_STATEMENT")
                           .categoryId("cat-RENT_STATEMENT")
                           .build())
            .documentType(AdditionalDocumentType.RENT_STATEMENT)
            .build();

        ListValue<AdditionalDocument> lv1 = ListValue.<AdditionalDocument>builder()
            .id("1").value(additionalDocument1).build();
        ListValue<AdditionalDocument> lv2 = ListValue.<AdditionalDocument>builder()
            .id("2").value(additionalDocument2).build();

        List<ListValue<AdditionalDocument>> additionalDocuments = List.of(lv1, lv2);

        when(pcsCase.getAdditionalDocuments()).thenReturn(additionalDocuments);

        // When
        underTest.createAllDocuments(pcsCase);

        // Then
        verify(documentRepository).saveAll(documentEntityListCaptor.capture());
        List<DocumentEntity> capturedEntities = documentEntityListCaptor.getValue();

        assertThat(capturedEntities).hasSize(2);

        assertThat(capturedEntities)
            .extracting(DocumentEntity::getFileName)
            .containsExactlyInAnyOrder("file-WITNESS_STATEMENT", "file-RENT_STATEMENT");

        assertThat(capturedEntities)
            .extracting(DocumentEntity::getType)
            .containsExactlyInAnyOrder(DocumentType.WITNESS_STATEMENT, DocumentType.RENT_STATEMENT);
    }

    @ParameterizedTest
    @EnumSource(AdditionalDocumentType.class)
    void shouldMapAllAdditionalDocumentTypes(AdditionalDocumentType additionalDocumentType) {
        // Given
        PCSCase pcsCase = mock(PCSCase.class);

        AdditionalDocument additionalDocument1 = AdditionalDocument.builder()
            .document(Document.builder().build())
            .documentType(additionalDocumentType)
            .build();

        List<ListValue<AdditionalDocument>> additionalDocuments = List.of(
            ListValue.<AdditionalDocument>builder().value(additionalDocument1).build()
        );

        when(pcsCase.getAdditionalDocuments()).thenReturn(additionalDocuments);

        // When
        underTest.createAllDocuments(pcsCase);

        // Then
        DocumentType expectedDocumentType = DocumentType.valueOf(additionalDocumentType.name());

        verify(documentRepository).saveAll(documentEntityListCaptor.capture());
        List<DocumentEntity> capturedEntities = documentEntityListCaptor.getValue();

        assertThat(capturedEntities)
            .extracting(DocumentEntity::getType)
            .containsExactly(expectedDocumentType);
    }

    @ParameterizedTest
    @MethodSource("additionalDocumentCategoryScenarios")
    void shouldMapAdditionalDocumentsToCaseFileCategories(AdditionalDocumentType additionalDocumentType,
                                                          CaseFileCategory expectedCategory) {
        // Given
        PCSCase pcsCase = mock(PCSCase.class);

        AdditionalDocument additionalDocument = AdditionalDocument.builder()
            .document(Document.builder().categoryId("uploaded-category").build())
            .documentType(additionalDocumentType)
            .build();

        when(pcsCase.getAdditionalDocuments()).thenReturn(List.of(
            ListValue.<AdditionalDocument>builder().value(additionalDocument).build()
        ));

        // When
        underTest.createAllDocuments(pcsCase);

        // Then
        verify(documentRepository).saveAll(documentEntityListCaptor.capture());
        List<DocumentEntity> capturedEntities = documentEntityListCaptor.getValue();

        assertThat(capturedEntities)
            .extracting(DocumentEntity::getCategoryId)
            .containsExactly(expectedCategory.getId());
    }

    @Test
    void shouldSaveRentStatementDocuments() {
        // Given
        PCSCase pcsCase = mock(PCSCase.class);
        Document doc = Document.builder()
            .url("url1")
            .filename("file1")
            .binaryUrl("bin1")
            .categoryId("cat1")
            .build();

        RentArrearsSection rentArrearsSection =  RentArrearsSection.builder()
            .statementDocuments(List.of(ListValue.<Document>builder().id("1").value(doc).build()))
            .build();

        when(pcsCase.getRentArrears()).thenReturn(rentArrearsSection);

        // When
        underTest.createAllDocuments(pcsCase);

        // Then
        verify(documentRepository).saveAll(documentEntityListCaptor.capture());
        List<DocumentEntity> entities = documentEntityListCaptor.getValue();
        assertThat(entities).hasSize(1);
        DocumentEntity entity = entities.getFirst();
        assertThat(entity.getType()).isEqualTo(DocumentType.RENT_STATEMENT);
        assertThat(entity.getFileName()).isEqualTo("file1");
        assertThat(entity.getCategoryId()).isEqualTo(CaseFileCategory.PROPERTY_DOCUMENTS.getId());
    }

    @Test
    void shouldSaveTenancyLicenceDocuments() {
        // Given
        PCSCase pcsCase = mock(PCSCase.class);
        Document doc = Document.builder()
            .url("url2")
            .filename("file2")
            .binaryUrl("bin2")
            .categoryId("cat2")
            .build();

        TenancyLicenceDetails tenancyLicenceDetails = TenancyLicenceDetails.builder()
            .tenancyLicenceDocuments(List.of(ListValue.<Document>builder().id("1").value(doc).build()))
            .build();

        when(pcsCase.getTenancyLicenceDetails()).thenReturn(tenancyLicenceDetails);

        // When
        underTest.createAllDocuments(pcsCase);

        // Then
        verify(documentRepository).saveAll(documentEntityListCaptor.capture());
        List<DocumentEntity> entities = documentEntityListCaptor.getValue();
        assertThat(entities).hasSize(1);
        DocumentEntity entity = entities.getFirst();
        assertThat(entity.getType()).isEqualTo(DocumentType.TENANCY_LICENCE);
        assertThat(entity.getFileName()).isEqualTo("file2");
        assertThat(entity.getCategoryId()).isEqualTo(CaseFileCategory.PROPERTY_DOCUMENTS.getId());
    }

    @Test
    void shouldSaveOccupationLicenceDocuments() {
        // Given
        PCSCase pcsCase = mock(PCSCase.class);

        Document doc = Document.builder()
            .url("url3")
            .filename("file3")
            .binaryUrl("bin3")
            .categoryId("cat3")
            .build();

        OccupationLicenceDetailsWales occupationLicenceDetails = OccupationLicenceDetailsWales.builder()
            .licenceDocuments(List.of(ListValue.<Document>builder().id("1").value(doc).build()))
            .build();

        when(pcsCase.getOccupationLicenceDetailsWales()).thenReturn(occupationLicenceDetails);

        // When
        underTest.createAllDocuments(pcsCase);

        // Then
        verify(documentRepository).saveAll(documentEntityListCaptor.capture());
        List<DocumentEntity> entities = documentEntityListCaptor.getValue();
        assertThat(entities).hasSize(1);
        DocumentEntity entity = entities.getFirst();
        assertThat(entity.getType()).isEqualTo(DocumentType.OCCUPATION_LICENCE);
        assertThat(entity.getFileName()).isEqualTo("file3");
        assertThat(entity.getCategoryId()).isEqualTo(CaseFileCategory.PROPERTY_DOCUMENTS.getId());
    }

    @Test
    void shouldSaveNoticeServedDocuments() {
        // Given
        PCSCase pcsCase = mock(PCSCase.class);

        Document doc = Document.builder()
            .url("url4")
            .filename("file4")
            .binaryUrl("bin4")
            .categoryId("cat4")
            .build();

        NoticeServedDetails noticeServedDetails = NoticeServedDetails.builder()
            .noticeDocuments(List.of(ListValue.<Document>builder().id("1").value(doc).build()))
            .build();

        when(pcsCase.getNoticeServedDetails()).thenReturn(noticeServedDetails);

        // When
        underTest.createAllDocuments(pcsCase);

        // Then
        verify(documentRepository).saveAll(documentEntityListCaptor.capture());
        List<DocumentEntity> entities = documentEntityListCaptor.getValue();
        assertThat(entities).hasSize(1);
        DocumentEntity entity = entities.getFirst();
        assertThat(entity.getType()).isEqualTo(DocumentType.NOTICE_FOR_SERVICE_OUT_OF_JURISDICTION);
        assertThat(entity.getFileName()).isEqualTo("file4");
        assertThat(entity.getCategoryId()).isEqualTo(CaseFileCategory.STATEMENTS_OF_CASE.getId());
    }

    @Test
    void shouldReturnEmptyListIfNoDocuments() {
        // Given
        PCSCase pcsCase = mock(PCSCase.class);
        when(documentRepository.saveAll(anyList())).thenReturn(List.of());

        // When
        List<DocumentEntity> entities = underTest.createAllDocuments(pcsCase);

        // Then
        assertThat(entities).isEmpty();
        verify(documentRepository).saveAll(anyList());
    }

    @ParameterizedTest
    @EnumSource(EvidenceDocumentType.class)
    void shouldMapAllEvidenceDocumentTypes(EvidenceDocumentType evidenceDocumentType) {
        // Given
        EvidenceOfDefendants evidenceDocument = EvidenceOfDefendants.builder()
                .document(Document.builder()
                        .url("url-WITNESS_STATEMENT")
                        .filename("file-WITNESS_STATEMENT")
                        .binaryUrl("bin-WITNESS_STATEMENT")
                        .categoryId("cat-WITNESS_STATEMENT")
                        .build())
                .documentType(evidenceDocumentType)
                .build();

        List<ListValue<EvidenceOfDefendants>> evidenceDocuments =
                List.of(ListValue.<EvidenceOfDefendants>builder()
                        .id("1").value(evidenceDocument).build());

        EnforcementOrder enforcementOrder = EnforcementOrder.builder()
                .warrantOfRestitutionDetails(WarrantOfRestitutionDetails.builder()
                        .additionalDocuments(evidenceDocuments)
                        .build())
                .build();

        // When
        underTest.createAllDocuments(enforcementOrder);

        // Then
        DocumentType expectedDocumentType = DocumentType.valueOf(evidenceDocumentType.name());

        verify(documentRepository).saveAll(documentEntityListCaptor.capture());
        List<DocumentEntity> capturedEntities = documentEntityListCaptor.getValue();

        assertThat(capturedEntities)
                .extracting(DocumentEntity::getType)
                .containsExactly(expectedDocumentType);
    }

    @Test
    void shouldHandleEmptyEvidenceDocument() {
        List<ListValue<EvidenceOfDefendants>> evidenceDocuments =
                new ArrayList<>();
        EnforcementOrder enforcementOrder = EnforcementOrder.builder()
                .warrantOfRestitutionDetails(WarrantOfRestitutionDetails.builder()
                        .additionalDocuments(evidenceDocuments)
                        .build())
                .build();

        // When
        underTest.createAllDocuments(enforcementOrder);

        // Then
        verify(documentRepository).saveAll(List.of());
    }

    @Test
    void shouldSaveDescriptionForAdditionalDocuments() {
        // Given
        PCSCase pcsCase = mock(PCSCase.class);

        AdditionalDocumentType additionalDocumentType =  AdditionalDocumentType.WITNESS_STATEMENT;
        String description = "A short description";

        AdditionalDocument additionalDocument1 = AdditionalDocument.builder()
                .document(Document.builder().build())
                .documentType(additionalDocumentType)
                .description(description)
                .build();

        List<ListValue<AdditionalDocument>> additionalDocuments = List.of(
                ListValue.<AdditionalDocument>builder().value(additionalDocument1).build()
        );

        when(pcsCase.getAdditionalDocuments()).thenReturn(additionalDocuments);

        // When
        underTest.createAllDocuments(pcsCase);

        // Then
        DocumentType expectedDocumentType = DocumentType.valueOf(additionalDocumentType.name());

        verify(documentRepository).saveAll(documentEntityListCaptor.capture());
        List<DocumentEntity> capturedEntities = documentEntityListCaptor.getValue();

        assertThat(capturedEntities)
                .extracting(DocumentEntity::getType)
                .containsExactly(expectedDocumentType);

        assertThat(capturedEntities)
                .extracting(DocumentEntity::getDescription)
                .containsExactly(description);
    }

    @Test
    void shouldConvertEmptyDescriptionToNull() {
        // Given
        PCSCase pcsCase = mock(PCSCase.class);

        AdditionalDocument additionalDocument = AdditionalDocument.builder()
                .document(Document.builder()
                        .url("url1")
                        .filename("file1")
                        .binaryUrl("bin1")
                        .categoryId("cat1")
                        .build())
                .documentType(AdditionalDocumentType.WITNESS_STATEMENT)
                .description("")
                .build();

        List<ListValue<AdditionalDocument>> additionalDocuments = List.of(
                ListValue.<AdditionalDocument>builder().value(additionalDocument).build()
        );

        when(pcsCase.getAdditionalDocuments()).thenReturn(additionalDocuments);

        // When
        underTest.createAllDocuments(pcsCase);

        // Then
        verify(documentRepository).saveAll(documentEntityListCaptor.capture());
        List<DocumentEntity> capturedEntities = documentEntityListCaptor.getValue();

        assertThat(capturedEntities).hasSize(1);
        assertThat(capturedEntities.getFirst().getDescription()).isNull();
    }

    @Test
    void shouldFilterOutNullValuesFromListValueDocuments() {
        // Given
        PCSCase pcsCase = mock(PCSCase.class);

        Document validDoc = Document.builder()
                .url("url1")
                .filename("file1")
                .binaryUrl("bin1")
                .categoryId("cat1")
                .build();

        List<ListValue<Document>> docsWithNull = List.of(
                ListValue.<Document>builder().id("1").value(validDoc).build(),
                ListValue.<Document>builder().id("2").value(null).build()
        );

        RentArrearsSection rentArrearsSection = RentArrearsSection.builder()
                .statementDocuments(docsWithNull)
                .build();

        when(pcsCase.getRentArrears()).thenReturn(rentArrearsSection);

        // When
        underTest.createAllDocuments(pcsCase);

        // Then
        verify(documentRepository).saveAll(documentEntityListCaptor.capture());
        List<DocumentEntity> entities = documentEntityListCaptor.getValue();
        assertThat(entities).hasSize(1);
        assertThat(entities.getFirst().getFileName()).isEqualTo("file1");
    }

    @Test
    void shouldSaveMultipleDocumentTypesInSingleCall() {
        // Given
        PCSCase pcsCase = mock(PCSCase.class);

        Document rentDoc = Document.builder()
                .url("url-rent").filename("file-rent").binaryUrl("bin-rent").categoryId("cat-rent").build();
        Document tenancyDoc = Document.builder()
                .url("url-tenancy").filename("file-tenancy").binaryUrl("bin-tenancy").categoryId("cat-tenancy").build();
        Document noticeDoc = Document.builder()
                .url("url-notice").filename("file-notice").binaryUrl("bin-notice").categoryId("cat-notice").build();

        when(pcsCase.getRentArrears()).thenReturn(RentArrearsSection.builder()
                .statementDocuments(List.of(ListValue.<Document>builder().id("1").value(rentDoc).build()))
                .build());
        when(pcsCase.getTenancyLicenceDetails()).thenReturn(TenancyLicenceDetails.builder()
                .tenancyLicenceDocuments(List.of(ListValue.<Document>builder().id("2").value(tenancyDoc).build()))
                .build());
        when(pcsCase.getNoticeServedDetails()).thenReturn(NoticeServedDetails.builder()
                .noticeDocuments(List.of(ListValue.<Document>builder().id("3").value(noticeDoc).build()))
                .build());

        // When
        underTest.createAllDocuments(pcsCase);

        // Then
        verify(documentRepository).saveAll(documentEntityListCaptor.capture());
        List<DocumentEntity> entities = documentEntityListCaptor.getValue();
        assertThat(entities).hasSize(3);

        assertThat(entities)
            .extracting(DocumentEntity::getType)
            .containsExactlyInAnyOrder(
                DocumentType.RENT_STATEMENT,
                DocumentType.TENANCY_LICENCE,
                DocumentType.NOTICE_FOR_SERVICE_OUT_OF_JURISDICTION
            );

        assertThat(entities)
            .extracting(DocumentEntity::getFileName)
            .containsExactlyInAnyOrder("file-rent", "file-tenancy", "file-notice");
    }

    @Test
    void shouldAllowNullDescriptionForDocumentsOtherThanAdditionalDocuments() {
        // Given
        PCSCase pcsCase = mock(PCSCase.class);

        Document doc = Document.builder()
                .build();

        NoticeServedDetails noticeServedDetails = NoticeServedDetails.builder()
                .noticeDocuments(List.of(ListValue.<Document>builder().id("1").value(doc).build()))
                .build();

        when(pcsCase.getNoticeServedDetails()).thenReturn(noticeServedDetails);

        // When
        underTest.createAllDocuments(pcsCase);

        // Then
        verify(documentRepository).saveAll(documentEntityListCaptor.capture());
        List<DocumentEntity> entities = documentEntityListCaptor.getValue();
        DocumentEntity entity = entities.getFirst();
        assertThat(entity.getDescription()).isNull();
    }

    private static Stream<Arguments> additionalDocumentCategoryScenarios() {
        return Stream.of(
            Arguments.of(
                AdditionalDocumentType.NOTICE_FOR_SERVICE_OUT_OF_JURISDICTION,
                CaseFileCategory.STATEMENTS_OF_CASE
            ),
            Arguments.of(AdditionalDocumentType.RENT_STATEMENT, CaseFileCategory.PROPERTY_DOCUMENTS),
            Arguments.of(AdditionalDocumentType.TENANCY_AGREEMENT, CaseFileCategory.PROPERTY_DOCUMENTS),
            Arguments.of(AdditionalDocumentType.POSSESSION_NOTICE, CaseFileCategory.PROPERTY_DOCUMENTS),
            Arguments.of(AdditionalDocumentType.WITNESS_STATEMENT, CaseFileCategory.EVIDENCE),
            Arguments.of(AdditionalDocumentType.CERTIFICATE_OF_SERVICE, CaseFileCategory.EVIDENCE),
            Arguments.of(AdditionalDocumentType.CORRESPONDENCE_FROM_DEFENDANT, CaseFileCategory.EVIDENCE),
            Arguments.of(AdditionalDocumentType.CORRESPONDENCE_FROM_CLAIMANT, CaseFileCategory.EVIDENCE),
            Arguments.of(AdditionalDocumentType.PHOTOGRAPHIC_EVIDENCE, CaseFileCategory.EVIDENCE),
            Arguments.of(AdditionalDocumentType.INSPECTION_OR_REPORT, CaseFileCategory.EVIDENCE),
            Arguments.of(AdditionalDocumentType.CERTIFICATE_OF_SUITABILITY_AS_LF, CaseFileCategory.CORRESPONDENCE),
            Arguments.of(AdditionalDocumentType.LEGAL_AID_CERTIFICATE, CaseFileCategory.CORRESPONDENCE),
            Arguments.of(AdditionalDocumentType.OTHER, CaseFileCategory.UNCATEGORISED)
        );
    }
}
