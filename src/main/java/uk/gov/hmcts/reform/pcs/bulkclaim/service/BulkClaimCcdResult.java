package uk.gov.hmcts.reform.pcs.bulkclaim.service;

import uk.gov.hmcts.reform.pcs.bulkclaim.model.BulkFailReason;
import uk.gov.hmcts.reform.pcs.bulkclaim.model.BulkValidationError;

import java.util.List;

/**
 * The outcome of asking CCD to create a bulk claim case. A failure is {@code retryable} when CCD may still have
 * created, or may yet create, the case, so the claim stays pending rather than being marked failed.
 */
public record BulkClaimCcdResult(Long caseReference,
                                 BulkFailReason failReason,
                                 List<BulkValidationError> errors,
                                 boolean retryable) {

    public static BulkClaimCcdResult created(long caseReference) {
        return new BulkClaimCcdResult(caseReference, null, List.of(), false);
    }

    public static BulkClaimCcdResult rejected(BulkFailReason reason, List<BulkValidationError> errors) {
        return new BulkClaimCcdResult(null, reason, errors, false);
    }

    public static BulkClaimCcdResult unavailable() {
        return new BulkClaimCcdResult(null, BulkFailReason.SAVE_FAILED, List.of(), true);
    }

    public boolean isCreated() {
        return caseReference != null;
    }
}
