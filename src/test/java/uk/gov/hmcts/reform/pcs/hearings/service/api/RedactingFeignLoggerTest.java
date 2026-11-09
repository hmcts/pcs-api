package uk.gov.hmcts.reform.pcs.hearings.service.api;

import static org.assertj.core.api.Assertions.assertThat;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import feign.Request;
import feign.RequestTemplate;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

class RedactingFeignLoggerTest {

    @Test
    void shouldRedactAuthenticationHeadersAndLogOtherRequestDetails() {
        Logger backingLogger = (Logger) LoggerFactory.getLogger(HmcHearingApi.class);
        Level originalLevel = backingLogger.getLevel();
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        backingLogger.addAppender(appender);
        backingLogger.setLevel(Level.DEBUG);

        // forcing commit
        try {
            Request request = Request.create(
                Request.HttpMethod.POST,
                "https://hmc.example/hearing",
                Map.of(
                    "Authorization", List.of("Bearer idam-token"),
                    "serviceauthorization", List.of("Bearer s2s-token"),
                    "hmctsDeploymentId", List.of("deployment-preview")
                ),
                "{\"caseRef\":\"1234\"}".getBytes(StandardCharsets.UTF_8),
                StandardCharsets.UTF_8,
                new RequestTemplate()
            );

            new TestableRedactingFeignLogger().log(request);

            String output = appender.list.stream()
                .map(ILoggingEvent::getFormattedMessage)
                .reduce("", (left, right) -> left + System.lineSeparator() + right);

            assertThat(output)
                .contains("Authorization: [REDACTED]")
                .contains("serviceauthorization: [REDACTED]")
                .contains("hmctsDeploymentId: deployment-preview")
                .contains("{\"caseRef\":\"1234\"}")
                .doesNotContain("idam-token", "s2s-token");
        } finally {
            backingLogger.detachAppender(appender);
            backingLogger.setLevel(originalLevel);
            appender.stop();
        }
    }

    private static final class TestableRedactingFeignLogger extends RedactingFeignLogger {

        private TestableRedactingFeignLogger() {
            super(HmcHearingApi.class);
        }

        private void log(Request request) {
            logRequest("HmcHearingApi#createHearing()", feign.Logger.Level.FULL, request);
        }
    }
}
