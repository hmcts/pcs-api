package uk.gov.hmcts.reform.pcs.notify.event;

import uk.gov.hmcts.reform.pcs.ccd.domain.respondpossessionclaim.DefendantResponseStatus;

import java.util.UUID;

public record DefendantResponseStatusUpdatedEvent(
    UUID entityId,
    DefendantResponseStatus previousStatus,
    DefendantResponseStatus newStatus
) {
}
