package uk.gov.hmcts.reform.pcs.noc.model;

import java.io.Serializable;
import java.util.UUID;

public record NocSideEffectTaskData(
    UUID jobId
) implements Serializable {
}
