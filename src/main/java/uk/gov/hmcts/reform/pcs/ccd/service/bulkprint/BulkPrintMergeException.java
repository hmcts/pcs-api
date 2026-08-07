package uk.gov.hmcts.reform.pcs.ccd.service.bulkprint;

import uk.gov.hmcts.reform.pcs.exception.ErrorCode;
import uk.gov.hmcts.reform.pcs.exception.RedactedRuntimeException;

/**
 * Thrown when the coversheet and pack documents cannot be merged into a single PDF for bulk print.
 * Non-terminal: {@link ClaimPackSender} records it as a send failure that self-heals on the next sweep.
 */
public class BulkPrintMergeException extends RedactedRuntimeException {

    public BulkPrintMergeException(ErrorCode errorCode, Throwable cause) {
        super(errorCode, cause);
    }
}
