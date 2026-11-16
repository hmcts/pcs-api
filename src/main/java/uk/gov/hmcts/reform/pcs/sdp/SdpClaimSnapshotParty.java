package uk.gov.hmcts.reform.pcs.sdp;

import java.util.UUID;

public record SdpClaimSnapshotParty(
    UUID claimId,
    UUID partyId,
    String role,
    boolean represented
) {
}
