package uk.gov.hmcts.reform.pcs.ccd.service.counterclaimform;

import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.docassembly.domain.OutputType;
import uk.gov.hmcts.reform.pcs.document.model.counterclaimform.CounterClaimFormPayload;
import uk.gov.hmcts.reform.pcs.document.service.DocAssemblyService;

@Service
public class CounterClaimFormDocumentGenerator {
    static final String TEMPLATE_ID = "CV-PCS-CLM-ENG-Counterclaim-Form.docx";
    static final String OUTPUT_FILENAME_PREFIX = "Counterclaim - Defendant ";

    private final DocAssemblyService docAssemblyService;

    public CounterClaimFormDocumentGenerator(DocAssemblyService docAssemblyService) {
        this.docAssemblyService = docAssemblyService;
    }

    public String generate(CounterClaimFormPayload payload, int defendantNumber) {
        return docAssemblyService.generateDocument(
            payload,
            TEMPLATE_ID,
            OutputType.PDF,
            OUTPUT_FILENAME_PREFIX + defendantNumber
        );
    }
}
