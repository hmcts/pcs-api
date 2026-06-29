package uk.gov.hmcts.reform.pcs.ccd.service.counterclaimform;

import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.docassembly.domain.OutputType;
import uk.gov.hmcts.reform.pcs.document.model.counterclaimform.CounterClaimFormPayload;
import uk.gov.hmcts.reform.pcs.document.service.DocAssemblyService;

/**
 * Thin Docmosis wrapper for the counter claim form. The filename carries the responding defendant's
 * position on the case, e.g. {@code Counterclaim - Defendant 1}.
 */
@Service
public class CounterClaimFormDocumentGenerator {

    static final String TEMPLATE_ID = "CV-PCS-CLM-ENG-Counterclaim-Form.docx";
    static final String OUTPUT_FILENAME_PREFIX = "Counterclaim - Defendant ";

    private final DocAssemblyService docAssemblyService;

    public CounterClaimFormDocumentGenerator(DocAssemblyService docAssemblyService) {
        this.docAssemblyService = docAssemblyService;
    }

    /**
     * Renders the counter claim form PDF via Docmosis and dg-docassembly.
     *
     * @param payload         populated payload (from {@link CounterClaimFormPayloadBuilder})
     * @param defendantNumber the responding defendant's rank on the case (1-based)
     * @return dm-store URL of the generated PDF
     */
    public String generate(CounterClaimFormPayload payload, int defendantNumber) {
        return docAssemblyService.generateDocument(
            payload,
            TEMPLATE_ID,
            OutputType.PDF,
            OUTPUT_FILENAME_PREFIX + defendantNumber
        );
    }
}
