package uk.gov.hmcts.reform.pcs.notify.event;

import uk.gov.hmcts.reform.pcs.ccd.domain.respondpossessionclaim.CounterClaimStatus;

import java.util.UUID;

public record CounterClaimStatusUpdatedEvent(
    UUID entityId,
    CounterClaimStatus previousStatus,
    CounterClaimStatus newStatus
) {
}
