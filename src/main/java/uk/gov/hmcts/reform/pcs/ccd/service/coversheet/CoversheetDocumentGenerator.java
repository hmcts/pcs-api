package uk.gov.hmcts.reform.pcs.ccd.service.coversheet;

import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.docassembly.domain.OutputType;
import uk.gov.hmcts.reform.pcs.document.model.coversheet.CoversheetPayload;
import uk.gov.hmcts.reform.pcs.document.service.DocAssemblyService;

/**
 * Thin Docmosis wrapper for the bulk-print coversheet. Rendered at send time and prepended to the pack;
 * not stored as a case document.
 */
@Service
public class CoversheetDocumentGenerator {

    static final String TEMPLATE_ID = "CV-PCS-LET-ENG-Coversheet.docx";
    static final String OUTPUT_FILENAME = "Coversheet";

    private final DocAssemblyService docAssemblyService;

    public CoversheetDocumentGenerator(DocAssemblyService docAssemblyService) {
        this.docAssemblyService = docAssemblyService;
    }

    public String generate(CoversheetPayload payload) {
        return docAssemblyService.generateDocument(payload, TEMPLATE_ID, OutputType.PDF, OUTPUT_FILENAME);
    }
}
