package uk.gov.hmcts.reform.pcs.notify.event;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import uk.gov.hmcts.reform.pcs.ccd.domain.respondpossessionclaim.DefendantResponseStatus;

import java.util.UUID;

@Getter
@RequiredArgsConstructor
public class DefendantResponseStatusUpdatedEvent {
    private final UUID entityId;
    private final DefendantResponseStatus previousStatus;
    private final DefendantResponseStatus newStatus;
}
