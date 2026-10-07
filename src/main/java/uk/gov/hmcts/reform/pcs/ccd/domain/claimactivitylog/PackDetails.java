package uk.gov.hmcts.reform.pcs.ccd.domain.claimactivitylog;

import uk.gov.hmcts.reform.pcs.ccd.service.bulkprint.LetterType;

import java.util.List;
import java.util.UUID;

/**
 * Detail for a {@code PACK_SENT} / {@code PACK_FAILED} row: one row per (recipient, pack) dispatch. The
 * pack is identified by (case, party, packType); {@code letterId} is Send Letter's receipt for the
 * dispatch, stored for traceability (reverse lookup in send-letter / testing-support download) — null on
 * failure (no letter was accepted). On failure, {@code failureReason} says why, {@code terminal} whether the
 * sweep should stop retrying, and {@code errorDetail} keeps the raw exception (class + message) so an
 * {@code UNKNOWN} failure is diagnosable from the audit row (mirrors {@link GenerationDetails}).
 */
public record PackDetails(LetterType packType,
                          List<PackDocumentRef> documents,
                          UUID letterId,
                          FailureReason failureReason,
                          Boolean terminal,
                          String errorDetail) implements ActivityDetails {

    private static final int MAX_ERROR_DETAIL = 500;

    public static PackDetails sent(LetterType packType, List<PackDocumentRef> documents, UUID letterId) {
        return new PackDetails(packType, documents, letterId, null, null, null);
    }

    /**
     * Build a failed-dispatch detail from the thrown exception: classifies the {@link FailureReason},
     * derives {@code terminal}, and captures the exception class + message (truncated).
     */
    public static PackDetails failed(LetterType packType, List<PackDocumentRef> documents, Throwable cause) {
        FailureReason reason = FailureReasons.from(cause);
        return new PackDetails(packType, documents, null, reason, reason.isTerminal(), describe(cause));
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
