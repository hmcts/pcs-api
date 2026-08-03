package uk.gov.hmcts.reform.pcs.ccd.service.bulkprint;

/**
 * Thrown when the coversheet and pack documents cannot be merged into a single PDF for bulk print.
 * Non-terminal: {@link ClaimPackSender} records it as a send failure that self-heals on the next sweep.
 */
public class BulkPrintMergeException extends RuntimeException {

    public BulkPrintMergeException(String message, Throwable cause) {
        super(message, cause);
    }
}
