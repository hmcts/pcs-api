package uk.gov.hmcts.reform.pcs.sdp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public record SdpClaimSnapshotHeader(
    UUID claimId,
    String claimantType,
    String againstTrespassers,
    String languageUsed,
    LocalDateTime claimSubmittedDate,
    LocalDateTime claimIssuedDate,
    Long caseReference,
    Integer caseManagementLocation,
    String legislativeCountry,
    String tenancyType,
    String servingMethod,
    LocalDate noticeDate,
    Integer snapshotVersion,
    String addressLine1,
    String addressLine2,
    String addressLine3,
    String postTown,
    String county,
    String country,
    String postcode
) {
}
