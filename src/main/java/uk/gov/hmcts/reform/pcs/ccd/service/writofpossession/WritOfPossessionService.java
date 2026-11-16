package uk.gov.hmcts.reform.pcs.ccd.service.writofpossession;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.pcs.ccd.service.document.DocumentImportService;
import uk.gov.hmcts.reform.pcs.document.model.writofpossession.WritOfPossessionPayload;

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

    static final String OUTPUT_FILENAME = "Writ of possession";

    private final WritOfPossessionPersistenceService persistenceService;
    private final WritOfPossessionDocumentGenerator documentGenerator;
    private final DocumentImportService documentImportService;

    public void generateAndAttach(UUID enforcementOrderId) {
        Optional<WritOfPossessionPayload> payload = persistenceService.buildPayload(enforcementOrderId);
        if (payload.isEmpty()) {
            return;
        }

        String documentUrl = documentGenerator.generate(payload.get(), OUTPUT_FILENAME);
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
