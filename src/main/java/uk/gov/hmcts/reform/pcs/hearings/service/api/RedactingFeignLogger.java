package uk.gov.hmcts.reform.pcs.hearings.service.api;

import feign.Logger;
import feign.Request;
import feign.slf4j.Slf4jLogger;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

class RedactingFeignLogger extends Slf4jLogger {

    static final String REDACTED = "[REDACTED]";

    RedactingFeignLogger(Class<?> loggerClass) {
        super(loggerClass);
    }

    @Override
    protected void logRequest(String configKey, Logger.Level logLevel, Request request) {
        Map<String, Collection<String>> redactedHeaders = new LinkedHashMap<>();
        request.headers().forEach((name, values) -> redactedHeaders.put(
            name,
            isSensitive(name) ? List.of(REDACTED) : values
        ));

        Request redactedRequest = Request.create(
            request.httpMethod(),
            request.url(),
            redactedHeaders,
            request.body(),
            request.charset(),
            request.requestTemplate()
        );
        super.logRequest(configKey, logLevel, redactedRequest);
    }

    private static boolean isSensitive(String headerName) {
        return "Authorization".equalsIgnoreCase(headerName)
            || HmcHearingApi.SERVICE_AUTHORIZATION.equalsIgnoreCase(headerName);
    }
}
