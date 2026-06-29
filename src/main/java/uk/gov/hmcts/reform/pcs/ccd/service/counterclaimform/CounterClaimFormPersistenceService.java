package uk.gov.hmcts.reform.pcs.ccd.service.counterclaimform;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.reform.pcs.ccd.domain.CaseFileCategory;
import uk.gov.hmcts.reform.pcs.ccd.domain.DocumentType;
import uk.gov.hmcts.reform.pcs.ccd.entity.DocumentEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.respondpossessionclaim.CounterClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.repository.CounterClaimRepository;
import uk.gov.hmcts.reform.pcs.ccd.repository.DocumentRepository;
import uk.gov.hmcts.reform.pcs.ccd.service.claimform.ClaimActivityLogService;
import uk.gov.hmcts.reform.pcs.ccd.service.document.DocumentImportService;
import uk.gov.hmcts.reform.pcs.document.model.counterclaimform.CounterClaimFormPayload;

import java.util.Optional;
import java.util.UUID;

/**
 * Transactional reads and writes for counter claim form generation, separated from the orchestration
 * in {@link CounterClaimFormService} so the Docmosis render runs outside any transaction. The render
 * context is built in a read-only transaction; the rendered document is attached in a short write
 * transaction. Both phases re-check idempotency so a re-run never attaches a second form.
 */
@Service
@Slf4j
public class CounterClaimFormPersistenceService {

    private final CounterClaimRepository counterClaimRepository;
    private final DocumentRepository documentRepository;
    private final CounterClaimFormPayloadBuilder payloadBuilder;
    private final DocumentImportService documentImportService;
    private final ClaimActivityLogService claimActivityLogService;

    public CounterClaimFormPersistenceService(CounterClaimRepository counterClaimRepository,
                                              DocumentRepository documentRepository,
                                              CounterClaimFormPayloadBuilder payloadBuilder,
                                              DocumentImportService documentImportService,
                                              ClaimActivityLogService claimActivityLogService) {
        this.counterClaimRepository = counterClaimRepository;
        this.documentRepository = documentRepository;
        this.payloadBuilder = payloadBuilder;
        this.documentImportService = documentImportService;
        this.claimActivityLogService = claimActivityLogService;
    }

    @Transactional(readOnly = true)
    public Optional<CounterClaimFormRenderContext> buildContextIfNotAttached(UUID counterClaimId) {
        CounterClaimEntity counterClaim = loadCounterClaim(counterClaimId);
        if (isAlreadyAttached(counterClaimId)) {
            log.info("Counter claim form already attached for counter claim {}, skipping", counterClaimId);
            return Optional.empty();
        }
        CounterClaimFormPayload payload = payloadBuilder.build(counterClaim);
        return Optional.of(new CounterClaimFormRenderContext(payload, defendantNumber(counterClaim)));
    }

    @Transactional
    public void attach(UUID counterClaimId, String dmStoreUrl) {
        CounterClaimEntity counterClaim = loadCounterClaim(counterClaimId);
        if (isAlreadyAttached(counterClaimId)) {
            log.info("Counter claim form already attached for counter claim {}, skipping attach", counterClaimId);
            return;
        }
        PcsCaseEntity pcsCase = counterClaim.getPcsCase();
        DocumentEntity document = documentImportService.addDocumentToCase(
            pcsCase, dmStoreUrl, CaseFileCategory.STATEMENTS_OF_CASE);
        document.setType(DocumentType.COUNTERCLAIM);
        document.setCounterClaim(counterClaim);
        document.setParty(counterClaim.getParty());
        claimActivityLogService.logGenerationSuccess(pcsCase, counterClaim.getParty());
    }

    @Transactional
    public void recordGenerationFailure(UUID counterClaimId) {
        CounterClaimEntity counterClaim = loadCounterClaim(counterClaimId);
        claimActivityLogService.logGenerationFailure(
            counterClaim.getPcsCase().getCaseReference(), counterClaim.getParty().getId());
    }

    private boolean isAlreadyAttached(UUID counterClaimId) {
        return documentRepository.existsByCounterClaim_IdAndType(counterClaimId, DocumentType.COUNTERCLAIM);
    }

    private CounterClaimEntity loadCounterClaim(UUID counterClaimId) {
        return counterClaimRepository.findById(counterClaimId)
            .orElseThrow(() -> new IllegalStateException(
                "No counter claim found for id: " + counterClaimId));
    }

    // TODO HDPI-6865: derive the defendant rank from claim_party.rank for counter_claim.party_id (AC02).
    private static int defendantNumber(CounterClaimEntity counterClaim) {
        return 1;
    }
}
