package uk.gov.hmcts.reform.pcs.ccd.domain.order;

import java.util.Objects;
import java.util.UUID;

/** Whether an order disposes of one of the case's general applications or lists it for a hearing. */
public record ApplicationDecision(UUID genAppId, GenAppDecision decision) {

    public ApplicationDecision {
        Objects.requireNonNull(genAppId, "genAppId");
        Objects.requireNonNull(decision, "decision");
    }
}
