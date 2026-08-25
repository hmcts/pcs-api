package uk.gov.hmcts.reform.pcs.document.service.exception;

import uk.gov.hmcts.reform.pcs.exception.ErrorCode;
import uk.gov.hmcts.reform.pcs.exception.RedactedRuntimeException;
import uk.gov.hmcts.reform.pcs.exception.RedactionContext;

/**
 * Thrown when storing/retrieving a generated document in the document store (CDAM/dm-store) fails.
 * Distinct from send-letter failures so the audit records {@code DOCUMENT_STORE_FAILED} rather than
 * misclassifying a store fault as a print-pipeline fault.
 */
public class DocumentStoreException extends RedactedRuntimeException {

    public DocumentStoreException(ErrorCode errorCode, RedactionContext redactionContext, Throwable cause) {
        super(errorCode, redactionContext, cause);
    }
}
