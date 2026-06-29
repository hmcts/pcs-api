package uk.gov.hmcts.reform.pcs.ccd.service.counterclaimform;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.pcs.ccd.service.document.DocumentImportService;

import java.util.Optional;
import java.util.UUID;

/**
 * Generates the counter claim form and attaches it to the case, where it shows under "Statements of
 * case".
 *
 * <p>Orchestration only: the Docmosis render runs outside any transaction, between a read-only
 * context build and a short write transaction that attaches the document — see
 * {@link CounterClaimFormPersistenceService}. Skips when the counter claim already has a form, so a
 * re-run never creates a second one.</p>
 */
@Service
@Slf4j
public class CounterClaimFormService {

    private final CounterClaimFormPersistenceService persistenceService;
    private final CounterClaimFormDocumentGenerator documentGenerator;
    private final DocumentImportService documentImportService;

    public CounterClaimFormService(CounterClaimFormPersistenceService persistenceService,
                                   CounterClaimFormDocumentGenerator documentGenerator,
                                   DocumentImportService documentImportService) {
        this.persistenceService = persistenceService;
        this.documentGenerator = documentGenerator;
        this.documentImportService = documentImportService;
    }

    public void generateAndAttach(UUID counterClaimId) {
        Optional<CounterClaimFormRenderContext> context =
            persistenceService.buildContextIfNotAttached(counterClaimId);
        if (context.isEmpty()) {
            return;
        }

        CounterClaimFormRenderContext renderContext = context.get();
        String dmStoreUrl = documentGenerator.generate(renderContext.payload(), renderContext.defendantNumber());
        try {
            persistenceService.attach(counterClaimId, dmStoreUrl);
        } catch (Exception e) {
            deleteOrphanedDocument(counterClaimId, dmStoreUrl);
            throw e;
        }
        log.info("Generated and attached counter claim form for counter claim {}: {}", counterClaimId, dmStoreUrl);
        // TODO HDPI-6865 (AC01): once generated, schedule a bulk-print job (BulkPrintScheduler is greenfield).
    }

    public void recordGenerationFailure(UUID counterClaimId) {
        try {
            persistenceService.recordGenerationFailure(counterClaimId);
        } catch (Exception e) {
            log.error("Failed to record counter claim form generation failure for counter claim {}",
                      counterClaimId, e);
        }
    }

    private void deleteOrphanedDocument(UUID counterClaimId, String dmStoreUrl) {
        try {
            documentImportService.deleteDocument(dmStoreUrl);
        } catch (Exception e) {
            log.error("Failed to delete orphaned counter claim form document for counter claim {}: {}",
                      counterClaimId, dmStoreUrl, e);
        }
    }
}
