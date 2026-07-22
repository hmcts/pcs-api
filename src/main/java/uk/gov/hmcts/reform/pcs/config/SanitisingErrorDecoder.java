package uk.gov.hmcts.reform.pcs.config;

import feign.Response;
import feign.codec.ErrorDecoder;
import uk.gov.hmcts.reform.pcs.exception.ErrorCode;
import uk.gov.hmcts.reform.pcs.exception.RedactionContext;
import uk.gov.hmcts.reform.pcs.exception.RemoteCallException;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public class SanitisingErrorDecoder implements ErrorDecoder {

    @Override
    public Exception decode(String methodKey, Response response) {
        return new RemoteCallException(ErrorCode.REMOTE_CALL,
                                       RedactionContext.builder()
                                           .value("Remote call", methodKey)
                                           .value("Status", response.status())
                                           .value("Response Body", safeReadBody(response))
                                           .build(), response.status());
    }

    private String safeReadBody(Response response) {
        if (response.body() == null) {
            return "<empty>";
        }
        try (InputStream is = response.body().asInputStream()) {
            byte[] bytes = is.readNBytes(4096);
            String body = new String(bytes, StandardCharsets.UTF_8);
            return bytes.length == 4096 ? body + "…(truncated)" : body;
        } catch (IOException e) {
            return "<unreadable>";
        }
    }

}
