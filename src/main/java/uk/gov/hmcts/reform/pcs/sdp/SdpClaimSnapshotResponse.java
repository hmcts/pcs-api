package uk.gov.hmcts.reform.pcs.sdp;

import java.time.LocalDateTime;
import java.util.UUID;

public record SdpClaimSnapshotResponse(
    UUID claimId,
    Integer responseId,
    UUID partyId,
    String makeCounterClaim,
    LocalDateTime responseSubmittedDate
) {
}
