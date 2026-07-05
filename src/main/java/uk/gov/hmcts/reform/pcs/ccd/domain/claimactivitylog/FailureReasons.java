package uk.gov.hmcts.reform.pcs.ccd.domain.claimactivitylog;

import feign.FeignException;
import uk.gov.hmcts.reform.pcs.ccd.service.bulkprint.BulkPrintMergeException;
import uk.gov.hmcts.reform.pcs.ccd.service.bulkprint.MissingPostalAddressException;
import uk.gov.hmcts.reform.pcs.document.service.exception.DocAssemblyException;
import uk.gov.hmcts.reform.pcs.exception.DocumentDownloadException;

/**
 * Maps the exceptions thrown by generation and pack dispatch to a {@link FailureReason} for the
 * {@code details} column. Keep in step with the error catalog (docs/architecture/pcs-error-catalog.md).
 */
public final class FailureReasons {

    private FailureReasons() {
    }

    public static FailureReason from(Throwable cause) {
        if (cause instanceof MissingPostalAddressException) {
            return FailureReason.MISSING_ADDRESS;
        }
        if (cause instanceof DocAssemblyException) {
            return FailureReason.RENDER_FAILED;
        }
        if (cause instanceof BulkPrintMergeException) {
            return FailureReason.MERGE_FAILED;
        }
        if (cause instanceof DocumentDownloadException) {
            return FailureReason.DOCUMENT_FETCH_FAILED;
        }
        if (cause instanceof FeignException) {
            return FailureReason.SEND_LETTER_UNAVAILABLE;
        }
        String message = String.valueOf(cause.getMessage());
        if (message.contains("case management location") || message.contains("AC06")) {
            return FailureReason.NO_COURT_LOCATION;
        }
        if (message.contains("download returned no content") || message.contains("truncated")) {
            return FailureReason.DOCUMENT_FETCH_FAILED;
        }
        return FailureReason.UNKNOWN;
    }
}
