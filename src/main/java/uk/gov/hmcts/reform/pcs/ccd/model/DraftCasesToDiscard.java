package uk.gov.hmcts.reform.pcs.ccd.model;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DraftCasesToDiscard {

    private final long caseReference;
}
