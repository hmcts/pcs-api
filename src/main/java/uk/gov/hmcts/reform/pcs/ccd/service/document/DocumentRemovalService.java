package uk.gov.hmcts.reform.pcs.ccd.service.document;

import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.pcs.ccd.entity.DocumentEntity;
import uk.gov.hmcts.reform.pcs.ccd.repository.DocumentRepository;
import uk.gov.hmcts.reform.pcs.exception.DocumentNotFoundException;
import uk.gov.hmcts.reform.pcs.exception.ErrorCode;
import uk.gov.hmcts.reform.pcs.exception.RedactionContext;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class DocumentRemovalService {

    private final DocumentRepository documentRepository;
    private final DocumentImportService documentImportService;

    public DocumentRemovalService(DocumentRepository documentRepository,
                                   DocumentImportService documentImportService) {
        this.documentRepository = documentRepository;
        this.documentImportService = documentImportService;
    }

    public void removeDocument(UUID documentEntityId, String reason) {
        DocumentEntity documentEntity = documentRepository.findById(documentEntityId)
            .orElseThrow(() -> new DocumentNotFoundException(ErrorCode.DOCUMENT_NOT_FOUND,
                RedactionContext.of("No document found with id: ", documentEntityId)));

        documentEntity.setRemoved(true);
        documentEntity.setRemovalReason(reason);
        documentEntity.setRemovedAt(LocalDateTime.now());
        documentRepository.save(documentEntity);

        documentImportService.deleteDocument(documentEntity.getUrl());
    }
}
