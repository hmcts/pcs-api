package uk.gov.hmcts.reform.pcs.ccd.service.claimform;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.sdk.SystemCaseEvent;
import uk.gov.hmcts.ccd.sdk.SystemCaseEventOutcome;
import uk.gov.hmcts.ccd.sdk.SystemCaseEventService;
import uk.gov.hmcts.reform.pcs.ccd.service.document.DocumentImportService;
import uk.gov.hmcts.reform.pcs.document.model.claimform.ClaimFormPayload;

import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.UUID;

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
    private final SystemCaseEventService systemCaseEventService;

    public ClaimFormService(ClaimFormPersistenceService persistenceService,
                            ClaimFormDocumentGenerator documentGenerator,
                            DocumentImportService documentImportService,
                            SystemCaseEventService systemCaseEventService) {
        this.persistenceService = persistenceService;
        this.documentGenerator = documentGenerator;
        this.documentImportService = documentImportService;
        this.systemCaseEventService = systemCaseEventService;
    }

    public void generateAndAttach(long caseReference) {
        Optional<ClaimFormPayload> payload = persistenceService.buildPayloadIfNotAttached(caseReference);
        if (payload.isEmpty()) {
            return;
        }

        String dmStoreUrl = documentGenerator.generate(payload.get());
        try {
            systemCaseEventService.submit(
                caseReference,
                new SystemCaseEvent("claimFormGenerated", "Claim form generated"),
                idempotencyKey(caseReference),
                context -> {
                    persistenceService.attach(caseReference, dmStoreUrl);
                    return SystemCaseEventOutcome.noStateChange();
                }
            );
        } catch (Exception e) {
            deleteOrphanedDocument(caseReference, dmStoreUrl);
            throw e;
        }
    }

    private UUID idempotencyKey(long caseReference) {
        return UUID.nameUUIDFromBytes(
            ("pcs:claim-form-generated:" + caseReference).getBytes(StandardCharsets.UTF_8)
        );
    }

    private void deleteOrphanedDocument(long caseReference, String dmStoreUrl) {
        try {
            documentImportService.deleteDocument(dmStoreUrl);
        } catch (Exception e) {
            log.error("Failed to delete orphaned claim form document for case {}: {}", caseReference, dmStoreUrl, e);
        }
    }
}
