package uk.gov.hmcts.reform.pcs.ccd.service.claimpack;

import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.docassembly.domain.OutputType;
import uk.gov.hmcts.reform.pcs.document.model.claimpack.ClaimPackFormPayload;
import uk.gov.hmcts.reform.pcs.document.service.DocAssemblyService;

/**
 * Thin Docmosis wrapper for the claim pack. Mirrors
 * {@link uk.gov.hmcts.reform.pcs.ccd.service.genapp.GenAppDocumentGenerator} but uses a constant
 * filename, since there is one claim pack per case.
 *
 * <p>A single template covers England and Wales; Welsh-only sections are gated by
 * {@code isWales} conditionals inside the {@code .docx}.</p>
 */
@Service
public class ClaimPackDocumentGenerator {

    static final String TEMPLATE_ID = "CV-PCS-CLM-ENG-Claim-Pack.docx";
    static final String OUTPUT_FILENAME = "Claim pack";

    private final DocAssemblyService docAssemblyService;

    public ClaimPackDocumentGenerator(DocAssemblyService docAssemblyService) {
        this.docAssemblyService = docAssemblyService;
    }

    /**
     * Renders the claim pack PDF via Docmosis and dg-docassembly.
     *
     * @param payload populated payload (typically from {@link ClaimPackPayloadBuilder})
     * @return dm-store URL of the generated PDF
     */
    public String generate(ClaimPackFormPayload payload) {
        return docAssemblyService.generateDocument(
            payload,
            TEMPLATE_ID,
            OutputType.PDF,
            OUTPUT_FILENAME
        );
    }

}
