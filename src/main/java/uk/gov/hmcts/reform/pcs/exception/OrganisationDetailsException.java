package uk.gov.hmcts.reform.pcs.exception;

public class OrganisationDetailsException extends RedactedRuntimeException {

    public OrganisationDetailsException(ErrorCode errorCode, Throwable cause) {
        super(errorCode, cause);
    }

}
