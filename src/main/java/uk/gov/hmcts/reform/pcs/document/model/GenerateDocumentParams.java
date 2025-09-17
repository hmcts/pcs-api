package uk.gov.hmcts.reform.pcs.document.model;

import lombok.Builder;
import lombok.Getter;
import uk.gov.hmcts.reform.docassembly.domain.OutputType;

@Builder
@Getter
public class GenerateDocumentParams {
    private String userAuthentication;
    private String serviceAuthentication;
    @Builder.Default
    private String templateId = "CV-SPC-CLM-ENG-01356.docx";
    private FormPayloadObj formPayload;
    @Builder.Default
    private OutputType outputType = OutputType.PDF;
    private String outputFilename;
    @Builder.Default
    private boolean secureDocStoreEnabled = false;
    private String caseTypeId;
    private String jurisdictionId;
}
