package uk.gov.hmcts.reform.pcs.ccd.service.coverletter;

import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.docassembly.domain.OutputType;
import uk.gov.hmcts.reform.pcs.document.model.coverletter.CoveringLetterPayload;
import uk.gov.hmcts.reform.pcs.document.service.DocAssemblyService;

@Service
public class CoveringLetterDocumentGenerator {

    static final String TEMPLATE_ID = "CV-PCS-LET-ENG-Covering-Letter.docx";
    static final String OUTPUT_FILENAME = "Covering Letter";

    private final DocAssemblyService docAssemblyService;

    public CoveringLetterDocumentGenerator(DocAssemblyService docAssemblyService) {
        this.docAssemblyService = docAssemblyService;
    }

    public String generate(CoveringLetterPayload payload) {
        return docAssemblyService.generateDocument(payload, TEMPLATE_ID, OutputType.PDF, OUTPUT_FILENAME);
    }
}
