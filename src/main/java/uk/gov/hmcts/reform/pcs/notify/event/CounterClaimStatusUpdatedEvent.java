package uk.gov.hmcts.reform.pcs.notify.event;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import uk.gov.hmcts.reform.pcs.ccd.domain.respondpossessionclaim.CounterClaimStatus;

import java.util.UUID;

@Getter
@RequiredArgsConstructor
public class CounterClaimStatusUpdatedEvent {
    private final UUID entityId;
    private final CounterClaimStatus previousStatus;
    private final CounterClaimStatus newStatus;
}
