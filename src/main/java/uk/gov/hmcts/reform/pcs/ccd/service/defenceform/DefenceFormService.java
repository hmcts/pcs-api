package uk.gov.hmcts.reform.pcs.ccd.service.defenceform;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.sdk.SystemCaseEvent;
import uk.gov.hmcts.ccd.sdk.SystemCaseEventOutcome;
import uk.gov.hmcts.ccd.sdk.SystemCaseEventService;
import uk.gov.hmcts.reform.pcs.ccd.service.document.DocumentImportService;

import java.nio.charset.StandardCharsets;
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
    private final SystemCaseEventService systemCaseEventService;

    public DefenceFormService(DefenceFormPersistenceService persistenceService,
                              DefenceFormDocumentGenerator documentGenerator,
                              DocumentImportService documentImportService,
                              SystemCaseEventService systemCaseEventService) {
        this.persistenceService = persistenceService;
        this.documentGenerator = documentGenerator;
        this.documentImportService = documentImportService;
        this.systemCaseEventService = systemCaseEventService;
    }

    public void generateAndAttach(Integer defendantResponseId) {
        Optional<DefenceFormRenderContext> context =
            persistenceService.buildContextIfNotAttached(defendantResponseId);
        if (context.isEmpty()) {
            return;
        }

        DefenceFormRenderContext renderContext = context.get();
        String dmStoreUrl = documentGenerator.generate(renderContext.payload(), renderContext.defendantNumber());
        try {
            systemCaseEventService.submit(
                renderContext.caseReference(),
                new SystemCaseEvent("defenceFormGenerated", "Defence form generated"),
                idempotencyKey(defendantResponseId),
                event -> {
                    persistenceService.attach(defendantResponseId, dmStoreUrl);
                    return SystemCaseEventOutcome.noStateChange();
                }
            );
        } catch (Exception e) {
            deleteOrphanedDocument(defendantResponseId, dmStoreUrl);
            throw e;
        }
    }

    private UUID idempotencyKey(Integer defendantResponseId) {
        return UUID.nameUUIDFromBytes(
            ("pcs:defence-form-generated:" + defendantResponseId).getBytes(StandardCharsets.UTF_8)
        );
    }

    private void deleteOrphanedDocument(Integer defendantResponseId, String dmStoreUrl) {
        try {
            documentImportService.deleteDocument(dmStoreUrl);
        } catch (Exception e) {
            log.error("Failed to delete orphaned defence form document for defendant response {}: {}",
                      defendantResponseId, dmStoreUrl, e);
        }
    }
}
