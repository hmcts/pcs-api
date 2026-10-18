package uk.gov.hmcts.reform.pcs.enforcement.batchprint;

/**
 * A case left out of a batch print, and why.
 */
public record ExcludedCase(String caseReference, BatchPrintExclusion code, String reason) {

    public static ExcludedCase of(String caseReference, BatchPrintExclusion exclusion) {
        return new ExcludedCase(caseReference, exclusion, exclusion.getReason());
    }
}
