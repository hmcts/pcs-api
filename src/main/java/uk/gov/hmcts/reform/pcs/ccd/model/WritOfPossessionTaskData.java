package uk.gov.hmcts.reform.pcs.ccd.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
public class WritOfPossessionTaskData {

    private final long caseReference;
    private final UUID enforcementOrderId;

}
