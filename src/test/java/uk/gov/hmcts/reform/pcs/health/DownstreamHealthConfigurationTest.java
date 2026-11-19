package uk.gov.hmcts.reform.pcs.health;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.boot.health.contributor.Status;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import uk.gov.hmcts.reform.ccd.client.CoreCaseDataApi;
import uk.gov.hmcts.reform.ccd.client.healthcheck.CoreCaseDataHealthIndicator;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class DownstreamHealthConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
        .withUserConfiguration(DownstreamHealthConfiguration.class)
        .withBean(CoreCaseDataApi.class, () -> mock(CoreCaseDataApi.class));

    private HttpServer downstream;
    private volatile int statusCode;
    private volatile String body;

    @BeforeEach
    void startDownstream() throws IOException {
        downstream = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        downstream.createContext("/health", exchange -> {
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(statusCode, bytes.length);
            try (OutputStream out = exchange.getResponseBody()) {
                out.write(bytes);
            }
        });
        downstream.start();
    }

    @AfterEach
    void stopDownstream() {
        downstream.stop(0);
    }

    @Test
    void shouldRegisterAllIndicatorsUnderTheirOldNamesWhenUrlsAreSet() {
        contextRunner
            .withPropertyValues(
                "core_case_data.api.url=http://localhost:4452",
                "payments.api.url=" + downstreamUrl(),
                "send-letter.url=" + downstreamUrl())
            .run(context -> assertThat(context)
                .hasSingleBean(CoreCaseDataHealthIndicator.class)
                .hasBean("coreCaseData")
                .hasBean("paymentsHealthIndicator")
                .hasBean("sendLetterHealthIndicator"));
    }

    @Test
    void shouldNotRegisterIndicatorsWithoutUrls() {
        contextRunner.run(context -> assertThat(context)
            .doesNotHaveBean(HealthIndicator.class)
            .doesNotHaveBean(CoreCaseDataHealthIndicator.class));
    }

    @Test
    void shouldReportDownstreamStatusAndIgnoreOtherFields() {
        respond(200, "{\"status\":\"UP\",\"components\":{\"db\":{\"status\":\"UP\"}}}");

        assertThat(paymentsIndicatorStatus()).isEqualTo(Status.UP);
        assertThat(sendLetterIndicatorStatus()).isEqualTo(Status.UP);
    }

    @Test
    void shouldReportDownWhenDownstreamReturnsAnError() {
        respond(503, "{\"status\":\"DOWN\"}");

        assertThat(paymentsIndicatorStatus()).isEqualTo(Status.DOWN);
        assertThat(sendLetterIndicatorStatus()).isEqualTo(Status.DOWN);
    }

    @Test
    void shouldReportDownWhenDownstreamIsUnreachable() {
        DownstreamHealthConfiguration config = new DownstreamHealthConfiguration();
        String closedPort = "http://localhost:1";

        assertThat(config.paymentsHealthIndicator(closedPort).health().getStatus()).isEqualTo(Status.DOWN);
        assertThat(config.sendLetterHealthIndicator(closedPort).health().getStatus()).isEqualTo(Status.DOWN);
    }

    private Status paymentsIndicatorStatus() {
        return new DownstreamHealthConfiguration().paymentsHealthIndicator(downstreamUrl()).health().getStatus();
    }

    private Status sendLetterIndicatorStatus() {
        return new DownstreamHealthConfiguration().sendLetterHealthIndicator(downstreamUrl()).health().getStatus();
    }

    private void respond(int statusCode, String body) {
        this.statusCode = statusCode;
        this.body = body;
    }

    private String downstreamUrl() {
        return "http://localhost:" + downstream.getAddress().getPort();
    }
}
