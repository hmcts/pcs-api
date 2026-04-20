package uk.gov.hmcts.reform.pcs.document.model;

import lombok.Builder;
import lombok.Getter;
import uk.gov.hmcts.reform.docassembly.domain.FormPayload;

@Builder
@Getter
public class GenAppFormPayload implements FormPayload {

    private String hwfReference;
    private String withoutNoticeReason;

    // From sample CV-SPC template
    private String ccdCaseReference;
    private String caseName;

}
