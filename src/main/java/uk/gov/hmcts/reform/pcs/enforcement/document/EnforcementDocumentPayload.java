package uk.gov.hmcts.reform.pcs.enforcement.document;

import lombok.Builder;
import lombok.Data;
import uk.gov.hmcts.reform.docassembly.domain.FormPayload;
import uk.gov.hmcts.reform.pcs.enforcement.domain.EnforcementOrder;

import java.time.LocalDateTime;

@Data
@Builder
public class EnforcementDocumentPayload implements FormPayload {

    private String documentTitle;
    private String caseReference;
    private String caseName;
    private String propertyAddress;
    private String enforcementType;
    private String enforcementNumber;
    private String seal;
    private String generatedAt;
    private String courtName;
    private String courtAddress;
    private String bailiffName;
    private String language;
    private LocalDateTime evictionDateTime;
    private Boolean dispenseWithService;
    private EnforcementOrder enforcementOrder;
}
