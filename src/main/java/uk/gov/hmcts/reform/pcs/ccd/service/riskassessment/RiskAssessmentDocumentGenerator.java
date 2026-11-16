package uk.gov.hmcts.reform.pcs.ccd.service.riskassessment;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.docassembly.domain.OutputType;
import uk.gov.hmcts.reform.pcs.document.model.riskassessment.RiskAssessmentPayload;
import uk.gov.hmcts.reform.pcs.document.service.DocAssemblyService;

@AllArgsConstructor
@Component
public class RiskAssessmentDocumentGenerator {

    static final String TEMPLATE_ID = "CV-PCS-APP-ENG-Risk-Assessment.docx";

    private final DocAssemblyService docAssemblyService;

    public String generate(RiskAssessmentPayload payload, String filename) {
        return docAssemblyService.generateDocument(payload, TEMPLATE_ID, OutputType.PDF, filename);
    }
}
