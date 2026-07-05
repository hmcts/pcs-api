package uk.gov.hmcts.reform.pcs.ccd.domain.claimactivitylog;

import uk.gov.hmcts.reform.pcs.ccd.service.bulkprint.LetterType;

import java.util.List;
import java.util.UUID;

/**
 * Detail for a {@code PACK_SENT} / {@code PACK_FAILED} row: one row per (recipient, pack) dispatch. The
 * pack is identified by (case, party, packType); {@code letterId} is Send Letter's receipt for the
 * dispatch, stored for traceability (reverse lookup in send-letter / testing-support download) — null on
 * failure (no letter was accepted). On failure, {@code failureReason} says why and {@code terminal}
 * whether the sweep should stop retrying.
 */
public record PackDetails(LetterType packType,
                          List<PackDocumentRef> documents,
                          UUID letterId,
                          FailureReason failureReason,
                          Boolean terminal) implements ActivityDetails {

    public static PackDetails sent(LetterType packType, List<PackDocumentRef> documents, UUID letterId) {
        return new PackDetails(packType, documents, letterId, null, null);
    }

    public static PackDetails failed(LetterType packType, List<PackDocumentRef> documents,
                                     FailureReason failureReason) {
        return new PackDetails(packType, documents, null, failureReason, failureReason.isTerminal());
    }
}
