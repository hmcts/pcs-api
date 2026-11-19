package uk.gov.hmcts.reform.pcs.bulkclaim.prepare;

import uk.gov.hmcts.reform.pcs.bulkclaim.model.BulkClaimPreparationResult;
import uk.gov.hmcts.reform.pcs.bulkclaim.xml.generated.RentalClaimSet;
import uk.gov.hmcts.reform.pcs.reference.dto.OrganisationDetailsResponse;

/**
 * Validates one rental claim from a v2 bulk request and derives everything needed to create it,
 * without calling CCD or writing to the database.
 */
public interface RentalBulkClaimPreparer {

    BulkClaimPreparationResult prepare(RentalClaimSet claimSet,
                                       RentalClaimSet.RentalClaim claim,
                                       OrganisationDetailsResponse organisation);
}
