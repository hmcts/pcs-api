package uk.gov.hmcts.reform.pcs.enforcement.batchprint;

public record ExcludedCase(String caseReference, BatchPrintExclusion code, String reason) {

    public static ExcludedCase of(String caseReference, BatchPrintExclusion exclusion) {
        return new ExcludedCase(caseReference, exclusion, exclusion.getReason());
    }
}
