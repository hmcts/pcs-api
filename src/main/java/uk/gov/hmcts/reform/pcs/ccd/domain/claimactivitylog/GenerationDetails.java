package uk.gov.hmcts.reform.pcs.ccd.domain.claimactivitylog;

import uk.gov.hmcts.reform.pcs.ccd.domain.DocumentType;

/**
 * Detail for a {@code DOCUMENTS_CREATED} FAILURE row: which document type failed to generate (no document
 * row exists to point at), why, and whether the scheduler has given up ({@code terminal} — the case will
 * never become ready without intervention).
 */
public record GenerationDetails(DocumentType documentType,
                                FailureReason failureReason,
                                boolean terminal) implements ActivityDetails {
}
