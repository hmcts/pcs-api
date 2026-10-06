package uk.gov.hmcts.reform.pcs.ccd.domain.bundling;

import java.time.LocalDate;
import java.util.UUID;

/**
 * One source document in a generated bundle, after em-stitching-api's BundleDocumentDTO, plus
 * where the SDK placed it in the stitched PDF.
 *
 * @param documentId the pcs document row the content came from
 * @param documentUrl the CDAM URL of the source document
 */
public record CaseBundleDocument(
    UUID documentId,
    String docTitle,
    String docDescription,
    String documentUrl,
    LocalDate date,
    int sortIndex,
    Integer startPage,
    Integer pageCount,
    String sha256
) {
}
