package uk.gov.hmcts.reform.pcs.ccd.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;

@Data
@Builder
@AllArgsConstructor
public class DeletionCaseData {

    private final long caseRef;
    private final State state;
}
