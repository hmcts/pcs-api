package uk.gov.hmcts.reform.pcs.config;

import feign.Response;
import feign.codec.ErrorDecoder;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.pcs.exception.RemoteCallException;

public class SanitisingErrorDecoder implements ErrorDecoder {

    @Override
    public Exception decode(String methodKey, Response response) {
        // Read status only. Deliberately do NOT touch response.body() —
        // pulling it into a message can expose sensitive data to logs
        int status = response.status();
        return RemoteCallException.create(methodKey, status);
    }

}
