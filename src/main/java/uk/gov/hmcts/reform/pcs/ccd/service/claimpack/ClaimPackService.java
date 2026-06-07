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
 * Builds the payload, renders the claim pack PDF via Docmosis, stores it, and attaches it to
 * the claim.
 *
 * <p>Runs in a transaction so the payload builder can read lazy JPA relations
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
     * Renders the claim pack and attaches it to the claim, where it shows under "Statements of
     * case". Skips generation when the claim already has a pack, so a re-run never creates a
     * second one.
     */
    @Transactional
    public void generateAndAttach(long caseReference) {
        PcsCaseEntity pcsCase = pcsCaseService.loadCase(caseReference);
        ClaimEntity claim = pcsCase.getClaims().getFirst();
        if (claim.getSubmissionDocument() != null) {
            log.info("Claim pack already attached for case {}, skipping", caseReference);
            return;
        }

        ClaimPackFormPayload payload = payloadBuilder.build(pcsCase);
        String dmStoreUrl = documentGenerator.generate(payload);
        DocumentEntity document = documentImportService.addDocumentToCase(
            caseReference, dmStoreUrl, CaseFileCategory.STATEMENTS_OF_CASE);
        claim.setSubmissionDocument(document);
        log.info("Generated and attached claim pack for case {}: {}", caseReference, dmStoreUrl);
    }

}
