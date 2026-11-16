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

    private final DocAssemblyService docAssemblyService;

    public String generate(WritOfPossessionPayload payload, String filename) {
        return docAssemblyService.generateDocument(payload, TEMPLATE_ID, OutputType.PDF, filename);
    }
}
