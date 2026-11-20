package uk.gov.hmcts.reform.pcs.bulkclaim.model;

import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;

/**
 * A bulk claim that passed validation, with every value the claim event needs already derived.
 */
public record PreparedBulkClaim(
    PCSCase pcsCase,
    BulkClaimExtras extras,
    String pbaAccountNumber,
    String pbaCustomerReference
) {
}
