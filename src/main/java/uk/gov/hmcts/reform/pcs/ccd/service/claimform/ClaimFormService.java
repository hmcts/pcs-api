package uk.gov.hmcts.reform.pcs.ccd.service.claimform;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.pcs.document.model.claimform.ClaimFormPayload;

import java.util.Optional;

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

    public ClaimFormService(ClaimFormPersistenceService persistenceService,
                            ClaimFormDocumentGenerator documentGenerator) {
        this.persistenceService = persistenceService;
        this.documentGenerator = documentGenerator;
    }

    public void generateAndAttach(long caseReference) {
        Optional<ClaimFormPayload> payload = persistenceService.buildPayloadIfNotAttached(caseReference);
        if (payload.isEmpty()) {
            return;
        }

        String dmStoreUrl = documentGenerator.generate(payload.get());
        persistenceService.attach(caseReference, dmStoreUrl);
        log.info("Generated and attached claim form for case {}: {}", caseReference, dmStoreUrl);
    }
}
