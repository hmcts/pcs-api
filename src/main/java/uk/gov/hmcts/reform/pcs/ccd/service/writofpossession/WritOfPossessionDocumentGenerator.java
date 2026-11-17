package uk.gov.hmcts.reform.pcs.ccd.service.writofpossession;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.docassembly.domain.OutputType;
import uk.gov.hmcts.reform.pcs.document.model.writofpossession.WritOfPossessionPayload;
import uk.gov.hmcts.reform.pcs.document.service.DocAssemblyService;

@AllArgsConstructor
@Component
public class WritOfPossessionDocumentGenerator {


    static final String TEMPLATE_ID = "CV-PCS-APP-ENG-Writ-Of-Possession.docx";
    static final String OUTPUT_FILENAME_TEMPLATE = "Writ of possession - application %d";

    private final DocAssemblyService docAssemblyService;

    public String generate(WritOfPossessionPayload payload, Integer rank) {
        return docAssemblyService.generateDocument(payload, TEMPLATE_ID, OutputType.PDF,
            OUTPUT_FILENAME_TEMPLATE.formatted(rank));
    }
}
