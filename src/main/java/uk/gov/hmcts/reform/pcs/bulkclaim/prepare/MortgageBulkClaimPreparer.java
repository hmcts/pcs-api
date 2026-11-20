package uk.gov.hmcts.reform.pcs.bulkclaim.prepare;

import uk.gov.hmcts.reform.pcs.bulkclaim.model.BulkClaimPreparationResult;
import uk.gov.hmcts.reform.pcs.bulkclaim.xml.generated.MortgageClaimSet;
import uk.gov.hmcts.reform.pcs.reference.dto.OrganisationDetailsResponse;

/**
 * Validates one mortgage claim from a v2 bulk request and derives everything needed to create it,
 * without calling CCD or writing to the database.
 */
public interface MortgageBulkClaimPreparer {

    BulkClaimPreparationResult prepare(MortgageClaimSet claimSet,
                                       MortgageClaimSet.MortgageClaim claim,
                                       OrganisationDetailsResponse organisation);
}
