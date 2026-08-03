package uk.gov.hmcts.reform.pcs.exception;

import org.springframework.http.HttpStatus;

import java.util.Set;

import static uk.gov.hmcts.reform.pcs.exception.ErrorCode.AUTH_TOKEN_EMPTY;
import static uk.gov.hmcts.reform.pcs.exception.ErrorCode.AUTH_TOKEN_RETRIEVAL_FAIL;

public class IdamException extends RedactedRuntimeException {

    public static final String OAUTH2_ERROR_CODE = "errorCode";
    public static final String AUTH_VALIDATION_STATUS_CODE = "statusCode";

    public IdamException(ErrorCode errorCode) {
        super(errorCode);
    }

    public IdamException(ErrorCode errorCode, RedactionContext redactionContext, Throwable cause) {
        super(errorCode, redactionContext, cause);
    }

    public boolean indicatesUpstreamUnavailable(Set<String> oauth2ThrottleErrorCodes) {
        if (AUTH_TOKEN_RETRIEVAL_FAIL == getCode()) {
            String errorCode = getContext().getValue(OAUTH2_ERROR_CODE)
                .map(String::valueOf)
                .orElse("empty");
            return oauth2ThrottleErrorCodes.contains(errorCode);
        }

        if (AUTH_TOKEN_EMPTY == getCode()) {
            return true;
        }

        int status = getContext().getValue(AUTH_VALIDATION_STATUS_CODE).map(o -> (int) o).orElse(0);
        // status < 0 => Feign received no response (timeout / connection refused).
        return status < 0
            || status == HttpStatus.TOO_MANY_REQUESTS.value()
            || status >= 500;
    }

}
