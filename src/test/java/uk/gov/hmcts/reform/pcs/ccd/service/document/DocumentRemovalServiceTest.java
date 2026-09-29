package uk.gov.hmcts.reform.pcs.ccd.service.document;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.pcs.ccd.entity.DocumentEntity;
import uk.gov.hmcts.reform.pcs.ccd.repository.DocumentRepository;
import uk.gov.hmcts.reform.pcs.exception.DocumentNotFoundException;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DocumentRemovalServiceTest {

    @Mock
    private DocumentRepository documentRepository;
    @Mock
    private DocumentImportService documentImportService;

    private DocumentRemovalService underTest;

    @BeforeEach
    void setUp() {
        underTest = new DocumentRemovalService(documentRepository, documentImportService);
    }

    @Test
    void shouldSoftDeleteDocumentAndRemoveFromDocumentStore() {
        UUID documentId = UUID.randomUUID();
        String reason = "Duplicate upload";
        DocumentEntity documentEntity = DocumentEntity.builder()
            .id(documentId)
            .url("http://dm-store/documents/" + documentId)
            .removed(false)
            .build();
        when(documentRepository.findById(documentId)).thenReturn(Optional.of(documentEntity));

        underTest.removeDocument(documentId, reason);

        ArgumentCaptor<DocumentEntity> captor = ArgumentCaptor.forClass(DocumentEntity.class);
        verify(documentRepository).save(captor.capture());
        DocumentEntity saved = captor.getValue();

        assertThat(saved.isRemoved()).isTrue();
        assertThat(saved.getRemovalReason()).isEqualTo(reason);
        assertThat(saved.getRemovedAt()).isNotNull();
        assertThat(saved.getRemovedAt()).isBeforeOrEqualTo(LocalDateTime.now());
    }

    @Test
    void shouldDeleteDocumentFromDocumentStoreUsingItsUrl() {
        UUID documentId = UUID.randomUUID();
        String url = "http://dm-store/documents/" + documentId;
        DocumentEntity documentEntity = DocumentEntity.builder()
            .id(documentId)
            .url(url)
            .build();
        when(documentRepository.findById(documentId)).thenReturn(Optional.of(documentEntity));

        underTest.removeDocument(documentId, "No longer relevant");

        verify(documentImportService).deleteDocument(url);
    }

    @Test
    void shouldThrowWhenDocumentNotFound() {
        UUID documentId = UUID.randomUUID();
        when(documentRepository.findById(documentId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> underTest.removeDocument(documentId, "reason"))
            .isInstanceOf(DocumentNotFoundException.class)
            .hasMessageContaining(documentId.toString());

        verifyNoInteractions(documentImportService);
        verify(documentRepository, never()).save(any());
    }
}
