package uk.gov.hmcts.reform.pcs.ccd.service.document;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
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
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
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
    void shouldFlushRemovalDetailsBeforeSoftDeletingDocument() {
        UUID documentId = UUID.randomUUID();
        String reason = "Duplicate upload";
        DocumentEntity document = DocumentEntity.builder()
            .id(documentId)
            .url("http://dm-store/documents/" + documentId)
            .build();
        when(documentRepository.findById(documentId)).thenReturn(Optional.of(document));
        LocalDateTime beforeRemoval = LocalDateTime.now();
        doAnswer(invocation -> {
            assertThat(document.getRemovalReason()).isEqualTo(reason);
            assertThat(document.getRemovedAt()).isBetween(beforeRemoval, LocalDateTime.now());
            return null;
        }).when(documentRepository).flush();

        underTest.removeDocument(documentId, reason);

        InOrder ordered = inOrder(documentRepository, documentImportService);
        ordered.verify(documentRepository).findById(documentId);
        ordered.verify(documentRepository).flush();
        ordered.verify(documentRepository).delete(document);
        ordered.verify(documentImportService).deleteDocument(document.getUrl());
        ordered.verifyNoMoreInteractions();
    }

    @Test
    void shouldRejectUnknownDocumentWithoutDeletingFromDocumentStore() {
        UUID documentId = UUID.randomUUID();
        when(documentRepository.findById(documentId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> underTest.removeDocument(documentId, "Duplicate upload"))
            .isInstanceOf(DocumentNotFoundException.class)
            .hasMessageContaining(documentId.toString());

        verify(documentRepository).findById(documentId);
        verifyNoMoreInteractions(documentRepository);
        verifyNoInteractions(documentImportService);
    }
}
