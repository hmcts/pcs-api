package uk.gov.hmcts.reform.pcs.ccd.service.claimform;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.reform.pcs.ccd.domain.CaseFileCategory;
import uk.gov.hmcts.reform.pcs.ccd.domain.DocumentType;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.DocumentEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.service.PcsCaseService;
import uk.gov.hmcts.reform.pcs.ccd.service.document.DocumentImportService;
import uk.gov.hmcts.reform.pcs.document.model.claimform.ClaimFormPayload;

import java.util.Optional;

/**
 * Transactional reads and writes for claim form generation, separated from the orchestration in
 * {@link ClaimFormService} so the Docmosis render runs outside any transaction. The payload is built
 * in a read-only transaction (so the builder can read lazy JPA relations), and the rendered document
 * is attached in a short write transaction. Both phases re-check idempotency so a re-run never
 * attaches a second pack.
 */
@Service
@Slf4j
public class ClaimFormPersistenceService {

    private final PcsCaseService pcsCaseService;
    private final ClaimFormPayloadBuilder payloadBuilder;
    private final DocumentImportService documentImportService;
    private final ClaimActivityLogService claimActivityLogService;

    public ClaimFormPersistenceService(PcsCaseService pcsCaseService,
                                       ClaimFormPayloadBuilder payloadBuilder,
                                       DocumentImportService documentImportService,
                                       ClaimActivityLogService claimActivityLogService) {
        this.pcsCaseService = pcsCaseService;
        this.payloadBuilder = payloadBuilder;
        this.documentImportService = documentImportService;
        this.claimActivityLogService = claimActivityLogService;
    }

    @Transactional(readOnly = true)
    public Optional<ClaimFormPayload> buildPayloadIfNotAttached(long caseReference) {
        PcsCaseEntity pcsCase = pcsCaseService.loadCase(caseReference);
        if (pcsCase.getClaims().getFirst().getClaimFormDocument() != null) {
            log.info("Claim form already attached for case {}, skipping", caseReference);
            return Optional.empty();
        }
        return Optional.of(payloadBuilder.build(pcsCase));
    }

    @Transactional
    public void attach(long caseReference, String dmStoreUrl) {
        ClaimEntity claim = pcsCaseService.loadCase(caseReference).getClaims().getFirst();
        if (claim.getClaimFormDocument() != null) {
            log.info("Claim form already attached for case {}, skipping attach", caseReference);
            return;
        }
        DocumentEntity document = documentImportService.addDocumentToCase(
            caseReference, dmStoreUrl, CaseFileCategory.STATEMENTS_OF_CASE);
        document.setType(DocumentType.CLAIM);
        claim.setClaimFormDocument(document);
        claimActivityLogService.logGenerationSuccess(caseReference);
    }
}
