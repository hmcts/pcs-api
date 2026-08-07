package uk.gov.hmcts.reform.pcs.exception;

public class InvalidPartyForAccessCodeException extends RedactedRuntimeException {

    public InvalidPartyForAccessCodeException(ErrorCode errorCode) {
        super(errorCode);
    }

    public InvalidPartyForAccessCodeException(ErrorCode errorCode, Throwable cause) {
        super(errorCode, cause);
    }
}

