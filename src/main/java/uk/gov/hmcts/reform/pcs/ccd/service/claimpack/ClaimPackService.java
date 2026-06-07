package uk.gov.hmcts.reform.pcs.ccd.service.claimpack;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.reform.pcs.ccd.domain.CaseFileCategory;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.DocumentEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.service.PcsCaseService;
import uk.gov.hmcts.reform.pcs.ccd.service.document.DocumentImportService;
import uk.gov.hmcts.reform.pcs.document.model.claimpack.ClaimPackFormPayload;

/**
 * Orchestrator for claim pack rendering — Commit 1 of plan §12.
 *
 * <p>Renders the PDF via Docmosis, stores it in CDAM/dm-store, and attaches the resulting document
 * to the claim via {@code DocumentImportService} + {@code ClaimEntity.submissionDocument}
 * (plan §12.8). Idempotent per the §3.1 invariant.</p>
 *
 * <p>Transactional so the payload builder can navigate lazy JPA relations
 * ({@code claim.noticeOfPossession}, {@code claim.rentArrears}, etc.) without
 * {@code LazyInitializationException}.</p>
 */
@Service
@Slf4j
public class ClaimPackService {

    private final PcsCaseService pcsCaseService;
    private final ClaimPackPayloadBuilder payloadBuilder;
    private final ClaimPackDocumentGenerator documentGenerator;
    private final DocumentImportService documentImportService;

    public ClaimPackService(PcsCaseService pcsCaseService,
                            ClaimPackPayloadBuilder payloadBuilder,
                            ClaimPackDocumentGenerator documentGenerator,
                            DocumentImportService documentImportService) {
        this.pcsCaseService = pcsCaseService;
        this.payloadBuilder = payloadBuilder;
        this.documentGenerator = documentGenerator;
        this.documentImportService = documentImportService;
    }

    /**
     * Build the payload, render the PDF via Docmosis, store it in CDAM/dm-store, and attach the
     * resulting document to the claim ({@code claim.submissionDocument}) so the case references its
     * own claim pack — and it lands in the "Statements of case" Case File View folder.
     *
     * <p>Consumer-side idempotency for the §3.1 invariant ("at most one pack per case"): a claim that
     * already has a submission document is a no-op, so a re-fired task never produces a second pack.</p>
     */
    @Transactional
    public void generateAndAttach(long caseReference) {
        PcsCaseEntity pcsCase = pcsCaseService.loadCase(caseReference);
        ClaimEntity claim = pcsCase.getClaims().getFirst();
        if (claim.getSubmissionDocument() != null) {
            log.info("Claim pack already attached for case {} — skipping regeneration", caseReference);
            return;
        }

        ClaimPackFormPayload payload = payloadBuilder.build(pcsCase);
        String dmStoreUrl = documentGenerator.generate(payload);
        DocumentEntity document = documentImportService.addDocumentToCase(
            caseReference, dmStoreUrl, CaseFileCategory.STATEMENTS_OF_CASE);
        claim.setSubmissionDocument(document);
        log.info("Generated and attached claim pack for case {} → {}", caseReference, dmStoreUrl);
    }

}
