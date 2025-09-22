package uk.gov.hmcts.reform.pcs.document.model;

import lombok.Data;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.docassembly.domain.FormPayload;


@Data
@Component
public class BaseFormPayload implements FormPayload {
    private String applicantName;
    private String caseNumber;
}
