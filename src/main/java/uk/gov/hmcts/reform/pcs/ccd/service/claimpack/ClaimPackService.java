package uk.gov.hmcts.reform.pcs.ccd.service.claimpack;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.service.PcsCaseService;
import uk.gov.hmcts.reform.pcs.document.model.claimpack.ClaimPackFormPayload;

/**
 * Orchestrator for claim pack rendering — Commit 1 of plan §12.
 *
 * <p>Returns the dm-store URL of the generated PDF and stops there. The next slice picks up
 * the URL and attaches it to the case via {@code DocumentImportService} +
 * {@code ClaimEntity.submissionDocument} (see plan §12.8).</p>
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

    public ClaimPackService(PcsCaseService pcsCaseService,
                            ClaimPackPayloadBuilder payloadBuilder,
                            ClaimPackDocumentGenerator documentGenerator) {
        this.pcsCaseService = pcsCaseService;
        this.payloadBuilder = payloadBuilder;
        this.documentGenerator = documentGenerator;
    }

    /**
     * Build the payload, render the PDF via Docmosis, return the dm-store URL.
     * No case mutation, no document attach — that's the next slice.
     */
    @Transactional(readOnly = true)
    public String generateAndRender(long caseReference) {
        PcsCaseEntity pcsCase = pcsCaseService.loadCase(caseReference);
        ClaimPackFormPayload payload = payloadBuilder.build(pcsCase);
        String dmStoreUrl = documentGenerator.generate(payload);
        log.info("Generated claim pack for case {} → {}", caseReference, dmStoreUrl);
        return dmStoreUrl;
    }

}
