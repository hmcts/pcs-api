package uk.gov.hmcts.reform.pcs.enforcement.batchprint;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.time.Instant;
import java.util.UUID;

public record PrintableDocument(BatchPrintDocumentKind kind,
                                String fileName,
                                Instant submittedDate,
                                @JsonIgnore UUID documentId) {
}
