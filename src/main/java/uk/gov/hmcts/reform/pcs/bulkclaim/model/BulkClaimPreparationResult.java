package uk.gov.hmcts.reform.pcs.bulkclaim.model;

import java.util.List;

public sealed interface BulkClaimPreparationResult {

    record Prepared(PreparedBulkClaim claim) implements BulkClaimPreparationResult {
    }

    record Rejected(String customerUniqueIdentifier, BulkFailReason reason, List<BulkValidationError> errors)
        implements BulkClaimPreparationResult {
    }
}
