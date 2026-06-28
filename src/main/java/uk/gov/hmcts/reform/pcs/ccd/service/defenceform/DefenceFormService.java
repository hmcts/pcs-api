package uk.gov.hmcts.reform.pcs.ccd.service.defenceform;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.pcs.ccd.service.document.DocumentImportService;

import java.util.Optional;
import java.util.UUID;

/**
 * Generates the defence form and attaches it to the case, where it shows under "Statements of case".
 *
 * <p>Orchestration only: the Docmosis render runs outside any transaction, between a read-only
 * context build and a short write transaction that attaches the document — see
 * {@link DefenceFormPersistenceService}. Skips when the response already has a document, so a re-run
 * never creates a second one.</p>
 */
@Service
@Slf4j
public class DefenceFormService {

    private final DefenceFormPersistenceService persistenceService;
    private final DefenceFormDocumentGenerator documentGenerator;
    private final DocumentImportService documentImportService;

    public DefenceFormService(DefenceFormPersistenceService persistenceService,
                              DefenceFormDocumentGenerator documentGenerator,
                              DocumentImportService documentImportService) {
        this.persistenceService = persistenceService;
        this.documentGenerator = documentGenerator;
        this.documentImportService = documentImportService;
    }

    public void generateAndAttach(UUID defendantResponseId) {
        Optional<DefenceFormRenderContext> context =
            persistenceService.buildContextIfNotAttached(defendantResponseId);
        if (context.isEmpty()) {
            return;
        }

        DefenceFormRenderContext renderContext = context.get();
        String dmStoreUrl = documentGenerator.generate(renderContext.payload(), renderContext.defendantNumber());
        try {
            persistenceService.attach(defendantResponseId, dmStoreUrl);
        } catch (Exception e) {
            deleteOrphanedDocument(defendantResponseId, dmStoreUrl);
            throw e;
        }
        log.info("Generated and attached defence form for defendant response {}: {}",
                 defendantResponseId, dmStoreUrl);
    }

    private void deleteOrphanedDocument(UUID defendantResponseId, String dmStoreUrl) {
        try {
            documentImportService.deleteDocument(dmStoreUrl);
        } catch (Exception e) {
            log.error("Failed to delete orphaned defence form document for defendant response {}: {}",
                      defendantResponseId, dmStoreUrl, e);
        }
    }
}
