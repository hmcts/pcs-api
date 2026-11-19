package uk.gov.hmcts.reform.pcs.bulkclaim.model;

import uk.gov.hmcts.reform.pcs.bulkclaim.entity.BulkClaimKind;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;

/**
 * A bulk claim that passed validation, with every value the claim event needs already derived.
 */
public record PreparedBulkClaim(
    String customerUniqueIdentifier,
    BulkClaimKind claimKind,
    String claimHash,
    PCSCase pcsCase,
    BulkClaimExtras extras,
    String pbaAccountNumber,
    String pbaCustomerReference
) {
}
