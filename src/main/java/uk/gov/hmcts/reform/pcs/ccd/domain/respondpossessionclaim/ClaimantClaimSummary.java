package uk.gov.hmcts.reform.pcs.ccd.domain.respondpossessionclaim;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** A case the signed-in citizen created as claimant, as listed on their account page. */
@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ClaimantClaimSummary {

    private String caseReference;

    /** The CCD case state, for example AWAITING_SUBMISSION_TO_HMCTS while the claim is still a draft. */
    private String state;

    /** The claim fee's payment status, for example PAID; null until the fee has been requested. */
    private String paymentStatus;

    private PropertyAddress propertyAddress;

    @Builder
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PropertyAddress {

        private String addressLine1;

        private String addressLine2;

        private String addressLine3;

        private String postTown;

        private String county;

        private String postcode;
    }
}
