package uk.gov.hmcts.reform.pcs.ccd.service.defenceform;

import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.docassembly.domain.OutputType;
import uk.gov.hmcts.reform.pcs.document.model.defenceform.DefenceFormPayload;
import uk.gov.hmcts.reform.pcs.document.service.DocAssemblyService;

/**
 * Thin Docmosis wrapper for the defence form. The filename carries the responding defendant's
 * position on the case, e.g. {@code Defence - Defendant 1}.
 */
@Service
public class DefenceFormDocumentGenerator {

    static final String TEMPLATE_ID = "CV-PCS-CLM-ENG-Defence-Form.docx";
    static final String OUTPUT_FILENAME_PREFIX = "Defence - Defendant ";

    private final DocAssemblyService docAssemblyService;

    public DefenceFormDocumentGenerator(DocAssemblyService docAssemblyService) {
        this.docAssemblyService = docAssemblyService;
    }

    /**
     * Renders the defence form PDF via Docmosis and dg-docassembly.
     *
     * @param payload          populated payload (typically from {@link DefenceFormPayloadBuilder})
     * @param defendantNumber  the responding defendant's rank on the case (1-based)
     * @return dm-store URL of the generated PDF
     */
    public String generate(DefenceFormPayload payload, int defendantNumber) {
        return docAssemblyService.generateDocument(
            payload,
            TEMPLATE_ID,
            OutputType.PDF,
            OUTPUT_FILENAME_PREFIX + defendantNumber
        );
    }

}
