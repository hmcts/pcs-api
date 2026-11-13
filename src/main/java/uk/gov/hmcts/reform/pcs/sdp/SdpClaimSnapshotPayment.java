package uk.gov.hmcts.reform.pcs.sdp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record SdpClaimSnapshotPayment(
    UUID claimId,
    Integer id,
    UUID partyId,
    String serviceRequestReference,
    String externalReference,
    BigDecimal amount,
    String paymentStatus,
    String paymentCallbackHandlerType,
    LocalDateTime requestDate
) {
}
