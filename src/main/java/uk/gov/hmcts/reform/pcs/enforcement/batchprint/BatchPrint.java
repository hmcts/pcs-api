package uk.gov.hmcts.reform.pcs.enforcement.batchprint;

/**
 * A finished batch print.
 *
 * @param pdf every printed case's documents in one PDF, or null when no case could be printed
 */
public record BatchPrint(byte[] pdf, BatchPrintPlan plan) {

    public boolean isEmpty() {
        return pdf == null;
    }
}
