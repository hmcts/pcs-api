package uk.gov.hmcts.reform.pcs.config;

import feign.Response;
import feign.codec.ErrorDecoder;
import uk.gov.hmcts.reform.pcs.exception.RemoteCallException;

public class SanitisingErrorDecoder implements ErrorDecoder {

    @Override
    public Exception decode(String methodKey, Response response) {
        // Read status only. Deliberately do NOT touch response.body() —
        // pulling it into a message could expose sensitive data to logs
        return RemoteCallException.create(methodKey, response.status());
    }

}
