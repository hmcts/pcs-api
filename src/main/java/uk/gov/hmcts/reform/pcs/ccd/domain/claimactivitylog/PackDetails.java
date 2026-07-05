package uk.gov.hmcts.reform.pcs.ccd.domain.claimactivitylog;

import uk.gov.hmcts.reform.pcs.ccd.service.bulkprint.LetterType;

import java.util.List;

/**
 * Detail for a {@code PACK_SENT} / {@code PACK_FAILED} row: one row per (recipient, pack) dispatch. The
 * pack is identified by (case, party, packType) — no send-letter letterId is stored by design. On failure,
 * {@code failureReason} says why and {@code terminal} whether the sweep should stop retrying.
 */
public record PackDetails(LetterType packType,
                          List<PackDocumentRef> documents,
                          FailureReason failureReason,
                          Boolean terminal) implements ActivityDetails {

    public static PackDetails sent(LetterType packType, List<PackDocumentRef> documents) {
        return new PackDetails(packType, documents, null, null);
    }

    public static PackDetails failed(LetterType packType, List<PackDocumentRef> documents,
                                     FailureReason failureReason) {
        return new PackDetails(packType, documents, failureReason, failureReason.isTerminal());
    }
}
