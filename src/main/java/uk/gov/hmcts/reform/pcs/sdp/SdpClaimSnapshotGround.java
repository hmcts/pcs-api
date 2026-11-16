package uk.gov.hmcts.reform.pcs.sdp;

import java.util.UUID;

public record SdpClaimSnapshotGround(
    UUID claimId,
    String code,
    String description,
    String category
) {
}
