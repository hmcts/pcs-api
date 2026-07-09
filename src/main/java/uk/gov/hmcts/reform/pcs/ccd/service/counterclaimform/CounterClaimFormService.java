package uk.gov.hmcts.reform.pcs.ccd.service.counterclaimform;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.pcs.ccd.service.document.DocumentImportService;

import java.util.Optional;
import java.util.UUID;

@Service
@Slf4j
@AllArgsConstructor
public class CounterClaimFormService {
    private final CounterClaimFormPersistenceService persistenceService;
    private final CounterClaimFormDocumentGenerator documentGenerator;
    private final DocumentImportService documentImportService;

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
        } catch (RuntimeException e) {
            deleteOrphanedDocument(counterClaimId, dmStoreUrl);
            throw e;
        }
    }

    public long recordGenerationFailure(UUID counterClaimId, Exception cause, boolean terminal) {
        try {
            return persistenceService.recordGenerationFailure(counterClaimId, cause, terminal);
        } catch (RuntimeException e) {
            log.error("Failed to record counter claim form generation failure for counter claim {}",
                      counterClaimId, e);
            return 0L;
        }
    }

    private void deleteOrphanedDocument(UUID counterClaimId, String dmStoreUrl) {
        try {
            documentImportService.deleteDocument(dmStoreUrl);
        } catch (RuntimeException e) {
            log.error("Failed to delete orphaned counter claim form document for counter claim {}: {}",
                      counterClaimId, dmStoreUrl, e);
        }
    }
}
