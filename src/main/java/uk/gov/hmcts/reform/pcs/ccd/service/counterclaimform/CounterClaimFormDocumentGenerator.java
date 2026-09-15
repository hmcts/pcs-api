package uk.gov.hmcts.reform.pcs.ccd.service.counterclaimform;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.docassembly.domain.OutputType;
import uk.gov.hmcts.reform.pcs.document.model.counterclaimform.CounterClaimFormPayload;
import uk.gov.hmcts.reform.pcs.document.service.DocAssemblyService;

@Service
@AllArgsConstructor
public class CounterClaimFormDocumentGenerator {
    static final String TEMPLATE_ID = "CV-PCS-CLM-ENG-Counterclaim-Form.docx";
    static final String LR_TEMPLATE_ID = "CV-PCS-CLM-ENG-Counterclaim-Form-LR.docx";
    static final String OUTPUT_FILENAME_PREFIX = "Counterclaim - Defendant ";

    private final DocAssemblyService docAssemblyService;

    public String generate(CounterClaimFormPayload payload, int defendantNumber) {
        return docAssemblyService.generateDocument(
            payload,
            templateId(payload),
            OutputType.PDF,
            OUTPUT_FILENAME_PREFIX + defendantNumber
        );
    }

    /** LR template when a legal representative completed the statement of truth, citizen template otherwise. */
    static String templateId(CounterClaimFormPayload payload) {
        return payload.isLegalRepresentative() ? LR_TEMPLATE_ID : TEMPLATE_ID;
    }
}
