package uk.gov.hmcts.reform.pcs.config;

import feign.Request;
import feign.Response;
import feign.codec.ErrorDecoder;
import uk.gov.hmcts.reform.pcs.exception.ErrorCode;
import uk.gov.hmcts.reform.pcs.exception.RedactionContext;
import uk.gov.hmcts.reform.pcs.exception.RemoteCallException;

import java.net.URI;
import java.util.Arrays;
import java.util.Objects;

public class SanitisingErrorDecoder implements ErrorDecoder {

    private static final String INVALID_URI = "<invalid-uri>";
    private static final String MISSING_HOST = "<missing-host>";

    @Override
    public Exception decode(String methodKey, Response response) {
        Request request = response.request();
        RedactionContext.RedactionContextBuilder builder = RedactionContext.builder()
            .value("Target", host(request))
            .value("Reason", response.reason())
            .value("Content-Type", header(response, "content-type"))
            .value("Content-Length", header(response, "content-length"))
            .value("Status", response.status())
            .value("Method", request.httpMethod())
            .value("MethodKey", methodKey);

        String correlationId = tracing(response);
        if (correlationId != null) {
            builder.value("Correlation Id", correlationId);
        }
        String retryAfter = header(response, "retry-after");
        if (retryAfter != null) {
            builder.value("Retry-After", retryAfter);
        }
        return new RemoteCallException(ErrorCode.REMOTE_CALL, builder.build(), response.status());
    }

    private String header(Response response, String name) {
        return response.headers().entrySet().stream()
            .filter(e -> e.getKey().equalsIgnoreCase(name))
            .flatMap(e -> e.getValue().stream()).findFirst().orElse(null);
    }

    private String tracing(Response response) {
        String[] names = {"x-correlation-id", "x-request-id", "request-id", "traceparent"};
        return Arrays.stream(names).map(name -> header(response, name))
            .filter(Objects::nonNull).findFirst().orElse(null);
    }

    private String host(Request request) {
        try {
            URI uri = URI.create(request.url());
            if (uri.getScheme() == null || uri.getHost() == null) {
                return MISSING_HOST;
            }
            String host = uri.getHost().contains(":") ? "[%s]".formatted(uri.getHost()) : uri.getHost();
            String port = uri.getPort() == -1 ? "" : ":%d".formatted(uri.getPort());
            return "%s://%s%s".formatted(uri.getScheme(), host, port);
        } catch (Exception e) {
            return INVALID_URI;
        }
    }

}
