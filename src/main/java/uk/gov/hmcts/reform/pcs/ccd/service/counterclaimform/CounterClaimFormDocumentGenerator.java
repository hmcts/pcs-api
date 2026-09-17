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
    static final String OUTPUT_FILENAME_TEMPLATE = "Counterclaim CC%d - Defendant %d";

    private final DocAssemblyService docAssemblyService;

    public String generate(CounterClaimFormPayload payload, int counterClaimRank, int defendantNumber) {
        String filename = String.format(OUTPUT_FILENAME_TEMPLATE, counterClaimRank, defendantNumber);
        return docAssemblyService.generateDocument(
            payload,
            TEMPLATE_ID,
            OutputType.PDF,
            filename
        );
    }
}
