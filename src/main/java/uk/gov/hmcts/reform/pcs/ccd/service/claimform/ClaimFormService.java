package uk.gov.hmcts.reform.pcs.ccd.service.claimform;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.pcs.ccd.service.document.DocumentImportService;
import uk.gov.hmcts.reform.pcs.document.model.claimform.ClaimFormPayload;

import java.util.Optional;
import java.util.function.Supplier;

/**
 * Generates the claim form and attaches it to the claim, where it shows under "Statements of case".
 *
 * <p>Orchestration only: the Docmosis render runs outside any transaction, between a read-only
 * payload build and a short write transaction that attaches the document — see
 * {@link ClaimFormPersistenceService}. Skips when the claim already has a pack, so a re-run never
 * creates a second one.</p>
 */
@Service
@Slf4j
public class ClaimFormService {

    private final ClaimFormPersistenceService persistenceService;
    private final ClaimFormDocumentGenerator documentGenerator;
    private final DocumentImportService documentImportService;

    public ClaimFormService(ClaimFormPersistenceService persistenceService,
                            ClaimFormDocumentGenerator documentGenerator,
                            DocumentImportService documentImportService) {
        this.persistenceService = persistenceService;
        this.documentGenerator = documentGenerator;
        this.documentImportService = documentImportService;
    }

    public void generateAndAttach(long caseReference) {
        Optional<ClaimFormPayload> payload = runStage(ClaimFormStage.PAYLOAD,
            () -> persistenceService.buildPayloadIfNotAttached(caseReference));
        if (payload.isEmpty()) {
            return;
        }

        String dmStoreUrl = runStage(ClaimFormStage.RENDER,
            () -> documentGenerator.generate(payload.get()));
        try {
            runStage(ClaimFormStage.STORE,
                () -> persistenceService.attach(caseReference, dmStoreUrl));
        } catch (Exception e) {
            deleteOrphanedDocument(caseReference, dmStoreUrl);
            throw e;
        }
        log.info("Generated and attached claim form for case {}: {}", caseReference, dmStoreUrl);
    }

    // Tags any failure with the stage it occurred in, so the terminal failure handler can report
    // whether it was the Docmosis render (RENDER) or the CDAM store (STORE) that broke.
    private static <T> T runStage(ClaimFormStage stage, Supplier<T> action) {
        try {
            return action.get();
        } catch (ClaimFormStageException e) {
            throw e;
        } catch (RuntimeException e) {
            throw new ClaimFormStageException(stage, e);
        }
    }

    private static void runStage(ClaimFormStage stage, Runnable action) {
        runStage(stage, () -> {
            action.run();
            return null;
        });
    }

    private void deleteOrphanedDocument(long caseReference, String dmStoreUrl) {
        try {
            documentImportService.deleteDocument(dmStoreUrl);
        } catch (Exception e) {
            log.error("Failed to delete orphaned claim form document for case {}: {}", caseReference, dmStoreUrl, e);
        }
    }
}
