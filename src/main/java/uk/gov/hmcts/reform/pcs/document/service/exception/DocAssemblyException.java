package uk.gov.hmcts.reform.pcs.document.service.exception;

import uk.gov.hmcts.reform.pcs.exception.ErrorCode;
import uk.gov.hmcts.reform.pcs.exception.PCSRuntimeException;

public class DocAssemblyException extends PCSRuntimeException {

    public DocAssemblyException(ErrorCode errorCode) {
        super(errorCode);
    }

    public DocAssemblyException(ErrorCode errorCode, Throwable cause) {
        super(errorCode, cause);
    }

}
