package uk.gov.hmcts.reform.pcs.document.model.counterclaimform;

import lombok.Builder;
import lombok.Data;
import uk.gov.hmcts.reform.docassembly.domain.FormPayload;

import java.time.LocalDate;

@Data
@Builder
public class CounterClaimFormPayload implements FormPayload {
    private String referenceNumber;
    private LocalDate issueDateSealed;
    private LocalDate submittedOn;

}
