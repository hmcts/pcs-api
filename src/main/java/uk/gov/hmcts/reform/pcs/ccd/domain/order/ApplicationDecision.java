package uk.gov.hmcts.reform.pcs.ccd.domain.order;

import java.util.Objects;
import java.util.UUID;

/** What an order decides about one of the case's general applications. */
public record ApplicationDecision(UUID genAppId, GenAppDecision decision) {

    public ApplicationDecision {
        Objects.requireNonNull(genAppId, "genAppId");
        Objects.requireNonNull(decision, "decision");
    }
}
