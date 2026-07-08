package uk.gov.hmcts.reform.pcs.ccd.domain.claimactivitylog;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Why a generation or pack dispatch failed, persisted as a string inside {@code details} (Java-side enum
 * only — adding/removing values is a code-only change, no migration). {@code terminal} = retrying cannot
 * succeed without intervention (dead-letter); non-terminal failures are retried by the sweep/scheduler.
 */
@Getter
@RequiredArgsConstructor
public enum FailureReason {

    /** No case management location / court resolvable. Data-fixable, retried until fixed. */
    NO_COURT_LOCATION(false),
    /** Docmosis / doc-assembly render failed. */
    RENDER_FAILED(false),
    /** Storing the generated document (CDAM/dm-store) failed. */
    DOCUMENT_STORE_FAILED(false),
    /** Fetching pack document bytes at send time failed. */
    DOCUMENT_FETCH_FAILED(false),
    /** Merging coversheet + pack documents into one PDF failed. */
    MERGE_FAILED(false),
    /** Send Letter service unavailable / rejected the letter. */
    SEND_LETTER_UNAVAILABLE(false),
    /** Recipient has no usable postal address — retrying cannot succeed. */
    MISSING_ADDRESS(true),
    UNKNOWN(false);

    private final boolean terminal;
}
