package uk.gov.hmcts.reform.pcs.sdp;

import java.time.LocalDateTime;
import java.util.UUID;

public record SdpClaimSnapshotPayment(
    UUID claimId,
    String externalReference,
    String paymentStatus,
    LocalDateTime requestDate
) {
}
