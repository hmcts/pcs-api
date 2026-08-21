package uk.gov.hmcts.reform.pcs.postcodecourt.exception;

import uk.gov.hmcts.reform.pcs.exception.ErrorCode;
import uk.gov.hmcts.reform.pcs.exception.RedactedRuntimeException;

public class InvalidPostCodeException extends RedactedRuntimeException {

    public InvalidPostCodeException(ErrorCode errorCode) {
        super(errorCode);
    }

}
