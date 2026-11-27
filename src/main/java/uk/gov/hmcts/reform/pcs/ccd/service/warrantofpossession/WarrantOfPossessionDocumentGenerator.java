package uk.gov.hmcts.reform.pcs.ccd.service.warrantofpossession;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.docassembly.domain.OutputType;
import uk.gov.hmcts.reform.pcs.document.model.warrantofpossession.WarrantOfPossessionPayload;
import uk.gov.hmcts.reform.pcs.document.service.DocAssemblyService;

@AllArgsConstructor
@Component
public class WarrantOfPossessionDocumentGenerator {

    static final String TEMPLATE_ID = "CV-PCS-APP-ENG-Warrant-Of-Possession.docx";
    static final String OUTPUT_FILENAME_TEMPLATE = "Warrant of possession - application %d";

    private final DocAssemblyService docAssemblyService;

    public String generate(WarrantOfPossessionPayload payload, Integer rank) {
        return docAssemblyService.generateDocument(payload, TEMPLATE_ID, OutputType.PDF,
            OUTPUT_FILENAME_TEMPLATE.formatted(rank));
    }
}
