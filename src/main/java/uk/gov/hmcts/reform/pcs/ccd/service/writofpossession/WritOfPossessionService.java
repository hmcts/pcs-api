package uk.gov.hmcts.reform.pcs.ccd.service.writofpossession;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.pcs.ccd.service.document.DocumentImportService;

import java.util.Optional;
import java.util.UUID;

/**
 * Generates the writ of possession application ({@code CV-PCS-APP-ENG-Writ-Of-Possession.docx}) for a submitted writ
 * enforcement order and attaches it to the case. One document per enforcement order.
 */
@Slf4j
@Service
@AllArgsConstructor
public class WritOfPossessionService {

    private final WritOfPossessionPersistenceService persistenceService;
    private final WritOfPossessionDocumentGenerator documentGenerator;
    private final DocumentImportService documentImportService;

    public void generateAndAttach(UUID enforcementOrderId) {
        Optional<WritOfPossessionRenderContext> context = persistenceService.buildRenderContext(enforcementOrderId);
        if (context.isEmpty()) {
            return;
        }

        String documentUrl = documentGenerator.generate(context.get().payload(), context.get().rank());
        try {
            persistenceService.attachDocumentAndQueueEmail(enforcementOrderId, documentUrl);
        } catch (Exception e) {
            deleteOrphanedDocument(enforcementOrderId, documentUrl);
            throw e;
        }
    }

    private void deleteOrphanedDocument(UUID enforcementOrderId, String documentUrl) {
        try {
            documentImportService.deleteDocument(documentUrl);
        } catch (Exception e) {
            log.error("Failed to delete orphaned writ of possession for enforcement order {}: {}",
                enforcementOrderId, documentUrl, e);
        }
    }
}
