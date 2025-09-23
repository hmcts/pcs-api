package uk.gov.hmcts.reform.pcs.document.model;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.docassembly.domain.FormPayload;


@Data
@Component
public class BaseFormPayload implements FormPayload {
    @NotBlank(message = "Applicant name is required")
    private String applicantName;

    @NotBlank(message = "Case number is required")
    private String caseNumber;
}
