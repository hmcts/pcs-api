package uk.gov.hmcts.reform.pcs.ccd.service.claimform;

import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.docassembly.domain.OutputType;
import uk.gov.hmcts.reform.pcs.document.model.claimform.ClaimFormPayload;
import uk.gov.hmcts.reform.pcs.document.service.DocAssemblyService;

/**
 * Thin Docmosis wrapper for the claim form. Mirrors
 * {@link uk.gov.hmcts.reform.pcs.ccd.service.genapp.GenAppDocumentGenerator} but uses a constant
 * filename, since there is one claim form per case.
 *
 * <p>A single template covers England and Wales; Welsh-only sections are gated by
 * {@code isWales} conditionals inside the {@code .docx}.</p>
 */
@Service
public class ClaimFormDocumentGenerator {

    static final String TEMPLATE_ID = "CV-PCS-CLM-ENG-Claim-Pack.docx";
    static final String OUTPUT_FILENAME = "Claim form";

    private final DocAssemblyService docAssemblyService;

    public ClaimFormDocumentGenerator(DocAssemblyService docAssemblyService) {
        this.docAssemblyService = docAssemblyService;
    }

    /**
     * Renders the claim form PDF via Docmosis and dg-docassembly.
     *
     * @param payload populated payload (typically from {@link ClaimFormPayloadBuilder})
     * @return dm-store URL of the generated PDF
     */
    public String generate(ClaimFormPayload payload) {
        return docAssemblyService.generateDocument(
            payload,
            TEMPLATE_ID,
            OutputType.PDF,
            OUTPUT_FILENAME
        );
    }

}
