package uk.gov.hmcts.reform.pcs.ccd.service.hearing;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.pcs.ccd.service.document.DocumentImportService;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Generates a notice of hearing for each recipient, e.g. "Notice of hearing 1 - Defendant 1", then adds them to
 * the case file and queues an email to each recipient. The notices are generated outside any transaction so no
 * database connection is held while Docmosis runs.
 */
@Service
@Slf4j
@AllArgsConstructor
public class HearingNoticeService {

    private final HearingNoticePersistenceService persistenceService;
    private final HearingNoticeDocumentGenerator documentGenerator;
    private final DocumentImportService documentImportService;

    public void generateAndAttach(Integer hearingId) {
        List<HearingNoticeRenderContext> contexts = persistenceService.buildNoticeContexts(hearingId);
        if (contexts.isEmpty()) {
            return;
        }

        Map<UUID, String> documentUrlsByPartyId = new LinkedHashMap<>();
        try {
            for (HearingNoticeRenderContext context : contexts) {
                documentUrlsByPartyId.put(context.partyId(),
                    documentGenerator.generate(context.payload(), context.filename()));
            }
            persistenceService.attachNoticesAndQueueEmails(hearingId, documentUrlsByPartyId);
        } catch (Exception e) {
            documentUrlsByPartyId.values().forEach(url -> deleteOrphanedDocument(hearingId, url));
            throw e;
        }
    }

    private void deleteOrphanedDocument(Integer hearingId, String documentUrl) {
        try {
            documentImportService.deleteDocument(documentUrl);
        } catch (Exception e) {
            log.error("Failed to delete orphaned notice of hearing for hearing {}: {}", hearingId, documentUrl, e);
        }
    }
}
