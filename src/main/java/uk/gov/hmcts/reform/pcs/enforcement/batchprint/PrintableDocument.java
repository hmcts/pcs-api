package uk.gov.hmcts.reform.pcs.enforcement.batchprint;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.time.Instant;
import java.util.UUID;

/**
 * One document chosen for a case: the latest of its kind in the case's Enforcement folder.
 *
 * @param documentId the dm-store id, used to fetch the document and never shown
 */
public record PrintableDocument(BatchPrintDocumentKind kind,
                                String fileName,
                                Instant submittedDate,
                                @JsonIgnore UUID documentId) {
}
