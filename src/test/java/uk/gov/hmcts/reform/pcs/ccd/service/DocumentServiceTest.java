package uk.gov.hmcts.reform.pcs.ccd.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.ccd.sdk.type.Document;
import uk.gov.hmcts.ccd.sdk.type.ListValue;
import uk.gov.hmcts.reform.pcs.ccd.domain.AdditionalDocument;
import uk.gov.hmcts.reform.pcs.ccd.domain.AdditionalDocumentType;
import uk.gov.hmcts.reform.pcs.ccd.domain.DocumentType;
import uk.gov.hmcts.reform.pcs.ccd.domain.NoticeServedDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.RentArrearsSection;
import uk.gov.hmcts.reform.pcs.ccd.domain.TenancyLicenceDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.wales.OccupationLicenceDetailsWales;
import uk.gov.hmcts.reform.pcs.ccd.entity.DocumentEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.repository.DocumentRepository;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DocumentServiceTest {

    @Mock
    private DocumentRepository documentRepository;

    @Mock
    private ObjectMapper objectMapper;

    @Captor
    private ArgumentCaptor<List<DocumentEntity>> documentEntityListCaptor;

    private DocumentService underTest;

    @BeforeEach
    void setUp() {
        underTest = new DocumentService(documentRepository, objectMapper);
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
        assertThat(entity.getType()).isEqualTo(DocumentType.NOTICE_SERVED);
        assertThat(entity.getFileName()).isEqualTo("file4");
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
                DocumentType.NOTICE_SERVED
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

    @Test
    void shouldSaveDefendantEvidenceDocuments() {
        // Given
        PcsCaseEntity pcsCase = mock(PcsCaseEntity.class);
        when(pcsCase.getCaseReference()).thenReturn(123L);

        Document doc1 = Document.builder()
            .url("url1").filename("file1.pdf").binaryUrl("bin1").categoryId("cat1").build();
        Document doc2 = Document.builder()
            .url("url2").filename("file2.pdf").binaryUrl("bin2").categoryId("cat2").build();

        List<ListValue<Document>> uploadedDocs = List.of(
            ListValue.<Document>builder().id("1").value(doc1).build(),
            ListValue.<Document>builder().id("2").value(doc2).build()
        );

        when(documentRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));

        // When
        List<DocumentEntity> result = underTest.createDefendantEvidenceDocuments(uploadedDocs, pcsCase);

        // Then
        verify(documentRepository).saveAll(documentEntityListCaptor.capture());
        List<DocumentEntity> entities = documentEntityListCaptor.getValue();
        assertThat(entities).hasSize(2);

        assertThat(entities).allSatisfy(entity -> {
            assertThat(entity.getType()).isEqualTo(DocumentType.DEFENDANT_EVIDENCE);
            assertThat(entity.getPcsCase()).isEqualTo(pcsCase);
        });

        assertThat(entities)
            .extracting(DocumentEntity::getFileName)
            .containsExactly("file1.pdf", "file2.pdf");

        assertThat(result).hasSize(2);
    }

    @Test
    void shouldReturnEmptyListWhenNoDefendantEvidenceDocuments() {
        // Given
        PcsCaseEntity pcsCase = mock(PcsCaseEntity.class);

        // When
        List<DocumentEntity> result = underTest.createDefendantEvidenceDocuments(null, pcsCase);

        // Then
        assertThat(result).isEmpty();
        verify(documentRepository, never()).saveAll(anyList());
    }

    @Test
    void shouldReturnEmptyListWhenDefendantEvidenceDocumentsIsEmpty() {
        // Given
        PcsCaseEntity pcsCase = mock(PcsCaseEntity.class);

        // When
        List<DocumentEntity> result = underTest.createDefendantEvidenceDocuments(
            Collections.emptyList(), pcsCase);

        // Then
        assertThat(result).isEmpty();
        verify(documentRepository, never()).saveAll(anyList());
    }

    @Test
    void shouldFilterOutNullValuesFromDefendantEvidenceDocuments() {
        // Given
        PcsCaseEntity pcsCase = mock(PcsCaseEntity.class);
        when(pcsCase.getCaseReference()).thenReturn(123L);

        Document validDoc = Document.builder()
            .url("url1").filename("file1.pdf").binaryUrl("bin1").categoryId("cat1").build();

        List<ListValue<Document>> uploadedDocs = List.of(
            ListValue.<Document>builder().id("1").value(validDoc).build(),
            ListValue.<Document>builder().id("2").value(null).build()
        );

        when(documentRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));

        // When
        underTest.createDefendantEvidenceDocuments(uploadedDocs, pcsCase);

        // Then
        verify(documentRepository).saveAll(documentEntityListCaptor.capture());
        List<DocumentEntity> entities = documentEntityListCaptor.getValue();
        assertThat(entities).hasSize(1);
        assertThat(entities.getFirst().getFileName()).isEqualTo("file1.pdf");
    }

    @Test
    void shouldExtractContentTypeFromCategoryIdJson() throws Exception {
        // Given
        PcsCaseEntity pcsCase = mock(PcsCaseEntity.class);
        when(pcsCase.getCaseReference()).thenReturn(123L);

        String categoryIdJson = "{\"mimeType\":\"application/pdf\",\"size\":1024}";
        Document doc = Document.builder()
            .url("url1").filename("file1.pdf").binaryUrl("bin1")
            .categoryId(categoryIdJson).build();

        List<ListValue<Document>> uploadedDocs = List.of(
            ListValue.<Document>builder().id("1").value(doc).build()
        );

        ObjectMapper realMapper = new ObjectMapper();
        DocumentService serviceWithRealMapper = new DocumentService(documentRepository, realMapper);
        when(documentRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));

        // When
        serviceWithRealMapper.createDefendantEvidenceDocuments(uploadedDocs, pcsCase);

        // Then
        verify(documentRepository).saveAll(documentEntityListCaptor.capture());
        List<DocumentEntity> entities = documentEntityListCaptor.getValue();
        assertThat(entities.getFirst().getContentType()).isEqualTo("application/pdf");
        assertThat(entities.getFirst().getSize()).isEqualTo(1024L);
    }

    @Test
    void shouldReturnNullContentTypeWhenCategoryIdIsNull() {
        // Given
        PcsCaseEntity pcsCase = mock(PcsCaseEntity.class);
        when(pcsCase.getCaseReference()).thenReturn(123L);

        Document doc = Document.builder()
            .url("url1").filename("file1.pdf").binaryUrl("bin1")
            .categoryId(null).build();

        List<ListValue<Document>> uploadedDocs = List.of(
            ListValue.<Document>builder().id("1").value(doc).build()
        );

        when(documentRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));

        // When
        underTest.createDefendantEvidenceDocuments(uploadedDocs, pcsCase);

        // Then
        verify(documentRepository).saveAll(documentEntityListCaptor.capture());
        List<DocumentEntity> entities = documentEntityListCaptor.getValue();
        assertThat(entities.getFirst().getContentType()).isNull();
        assertThat(entities.getFirst().getSize()).isNull();
    }

    @Test
    void shouldReturnNullContentTypeWhenCategoryIdIsBlank() {
        // Given
        PcsCaseEntity pcsCase = mock(PcsCaseEntity.class);
        when(pcsCase.getCaseReference()).thenReturn(123L);

        Document doc = Document.builder()
            .url("url1").filename("file1.pdf").binaryUrl("bin1")
            .categoryId("   ").build();

        List<ListValue<Document>> uploadedDocs = List.of(
            ListValue.<Document>builder().id("1").value(doc).build()
        );

        when(documentRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));

        // When
        underTest.createDefendantEvidenceDocuments(uploadedDocs, pcsCase);

        // Then
        verify(documentRepository).saveAll(documentEntityListCaptor.capture());
        List<DocumentEntity> entities = documentEntityListCaptor.getValue();
        assertThat(entities.getFirst().getContentType()).isNull();
        assertThat(entities.getFirst().getSize()).isNull();
    }

    @Test
    void shouldReturnNullContentTypeWhenCategoryIdIsNotJson() throws Exception {
        // Given
        PcsCaseEntity pcsCase = mock(PcsCaseEntity.class);
        when(pcsCase.getCaseReference()).thenReturn(123L);

        Document doc = Document.builder()
            .url("url1").filename("file1.pdf").binaryUrl("bin1")
            .categoryId("not-json").build();

        List<ListValue<Document>> uploadedDocs = List.of(
            ListValue.<Document>builder().id("1").value(doc).build()
        );

        ObjectMapper realMapper = new ObjectMapper();
        DocumentService serviceWithRealMapper = new DocumentService(documentRepository, realMapper);
        when(documentRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));

        // When
        serviceWithRealMapper.createDefendantEvidenceDocuments(uploadedDocs, pcsCase);

        // Then
        verify(documentRepository).saveAll(documentEntityListCaptor.capture());
        List<DocumentEntity> entities = documentEntityListCaptor.getValue();
        assertThat(entities.getFirst().getContentType()).isNull();
        assertThat(entities.getFirst().getSize()).isNull();
    }

    @Test
    void shouldReturnNullWhenJsonHasNoMimeTypeOrSizeFields() throws Exception {
        // Given
        PcsCaseEntity pcsCase = mock(PcsCaseEntity.class);
        when(pcsCase.getCaseReference()).thenReturn(123L);

        Document doc = Document.builder()
            .url("url1").filename("file1.pdf").binaryUrl("bin1")
            .categoryId("{\"other\":\"value\"}").build();

        List<ListValue<Document>> uploadedDocs = List.of(
            ListValue.<Document>builder().id("1").value(doc).build()
        );

        ObjectMapper realMapper = new ObjectMapper();
        DocumentService serviceWithRealMapper = new DocumentService(documentRepository, realMapper);
        when(documentRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));

        // When
        serviceWithRealMapper.createDefendantEvidenceDocuments(uploadedDocs, pcsCase);

        // Then
        verify(documentRepository).saveAll(documentEntityListCaptor.capture());
        List<DocumentEntity> entities = documentEntityListCaptor.getValue();
        assertThat(entities.getFirst().getContentType()).isNull();
        assertThat(entities.getFirst().getSize()).isNull();
    }
}
