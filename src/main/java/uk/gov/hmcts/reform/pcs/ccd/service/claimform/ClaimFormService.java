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

/**
 * Builds the payload, renders the claim form PDF via Docmosis, stores it, and attaches it to
 * the claim.
 *
 * <p>Runs in a transaction so the payload builder can read lazy JPA relations
 * ({@code claim.noticeOfPossession}, {@code claim.rentArrears}, etc.) without
 * {@code LazyInitializationException}.</p>
 */
@Service
@Slf4j
public class ClaimFormService {

    private final PcsCaseService pcsCaseService;
    private final ClaimFormPayloadBuilder payloadBuilder;
    private final ClaimFormDocumentGenerator documentGenerator;
    private final DocumentImportService documentImportService;
    private final ClaimActivityLogService claimActivityLogService;

    public ClaimFormService(PcsCaseService pcsCaseService,
                            ClaimFormPayloadBuilder payloadBuilder,
                            ClaimFormDocumentGenerator documentGenerator,
                            DocumentImportService documentImportService,
                            ClaimActivityLogService claimActivityLogService) {
        this.pcsCaseService = pcsCaseService;
        this.payloadBuilder = payloadBuilder;
        this.documentGenerator = documentGenerator;
        this.documentImportService = documentImportService;
        this.claimActivityLogService = claimActivityLogService;
    }

    /**
     * Renders the claim form and attaches it to the claim, where it shows under "Statements of
     * case". Skips generation when the claim already has a pack, so a re-run never creates a
     * second one.
     */
    @Transactional
    public void generateAndAttach(long caseReference) {
        PcsCaseEntity pcsCase = pcsCaseService.loadCase(caseReference);
        ClaimEntity claim = pcsCase.getClaims().getFirst();
        if (claim.getClaimFormDocument() != null) {
            log.info("Claim form already attached for case {}, skipping", caseReference);
            return;
        }

        ClaimFormPayload payload = payloadBuilder.build(pcsCase);
        String dmStoreUrl = documentGenerator.generate(payload);
        DocumentEntity document = documentImportService.addDocumentToCase(
            caseReference, dmStoreUrl, CaseFileCategory.STATEMENTS_OF_CASE);
        document.setType(DocumentType.CLAIM);
        claim.setClaimFormDocument(document);
        // Success log shares this transaction, so the pack and its activity-log row commit together.
        claimActivityLogService.logGenerationSuccess(caseReference);
        log.info("Generated and attached claim form for case {}: {}", caseReference, dmStoreUrl);
    }

}
