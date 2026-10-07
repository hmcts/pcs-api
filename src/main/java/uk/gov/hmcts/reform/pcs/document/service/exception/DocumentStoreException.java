package uk.gov.hmcts.reform.pcs.document.service.exception;

/**
 * Thrown when storing/retrieving a generated document in the document store (CDAM/dm-store) fails.
 * Distinct from send-letter failures so the audit records {@code DOCUMENT_STORE_FAILED} rather than
 * misclassifying a store fault as a print-pipeline fault.
 */
public class DocumentStoreException extends RuntimeException {

    public DocumentStoreException(String message, Throwable cause) {
        super(message, cause);
    }
}
