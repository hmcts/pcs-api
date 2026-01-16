package uk.gov.hmcts.reform.pcs.ccd.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
import uk.gov.hmcts.reform.pcs.ccd.repository.DocumentRepository;

import java.util.List;

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
}
