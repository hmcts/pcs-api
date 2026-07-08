package uk.gov.hmcts.reform.pcs.ccd.domain.claimactivitylog;

import uk.gov.hmcts.reform.pcs.ccd.domain.DocumentType;

/**
 * Detail for a {@code DOCUMENTS_CREATED} FAILURE row: which document type failed to generate (no document
 * row exists to point at), the classified {@link FailureReason}, whether the scheduler has given up
 * ({@code terminal}), and {@code errorDetail} — the raw exception class + message. The classified reason is
 * a coarse enum; {@code errorDetail} keeps the actual exception so an {@code UNKNOWN} failure is still
 * diagnosable from the audit row without re-deriving it from the logs.
 */
public record GenerationDetails(DocumentType documentType,
                                FailureReason failureReason,
                                boolean terminal,
                                String errorDetail) implements ActivityDetails {

    private static final int MAX_ERROR_DETAIL = 500;

    /**
     * Build the failure detail from the thrown exception: classifies the {@link FailureReason} and captures
     * the exception class + message (truncated) so the reason enum is backed by the concrete error.
     */
    public static GenerationDetails forFailure(DocumentType documentType, Throwable cause, boolean terminal) {
        return new GenerationDetails(documentType, FailureReasons.from(cause), terminal, describe(cause));
    }

    private static String describe(Throwable cause) {
        if (cause == null) {
            return null;
        }
        String message = cause.getMessage();
        String detail = cause.getClass().getSimpleName() + (message == null ? "" : ": " + message);
        return detail.length() > MAX_ERROR_DETAIL ? detail.substring(0, MAX_ERROR_DETAIL) : detail;
    }
}
