package uk.gov.hmcts.reform.pcs.ccd.domain.citizenclaim;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

/**
 * The confirmation body of a citizen claim's final submit, read by the frontend to take payment.
 */
@Builder
@Data
@AllArgsConstructor
public class CitizenPossessionClaimSubmitResponse {

    private final String serviceRequestReference;

    // Pence, as a string, matching the MoneyGBP fields of citizenPossessionClaim
    private final String feeAmount;

}
