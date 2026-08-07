package uk.gov.hmcts.reform.pcs.postcodecourt.exception;

import uk.gov.hmcts.reform.pcs.exception.ErrorCode;
import uk.gov.hmcts.reform.pcs.exception.RedactedRuntimeException;
import uk.gov.hmcts.reform.pcs.exception.RedactionContext;

public class EligibilityCheckException extends RedactedRuntimeException {

    public EligibilityCheckException(ErrorCode errorCode, RedactionContext redactionContext) {
        super(errorCode, redactionContext);
    }

}
