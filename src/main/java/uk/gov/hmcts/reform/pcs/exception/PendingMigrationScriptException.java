package uk.gov.hmcts.reform.pcs.exception;

public class PendingMigrationScriptException extends RedactedRuntimeException {

    public PendingMigrationScriptException(ErrorCode errorCode, RedactionContext redactionContext) {
        super(errorCode, redactionContext);
    }
}
