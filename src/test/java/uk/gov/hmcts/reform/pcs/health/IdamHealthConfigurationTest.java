package uk.gov.hmcts.reform.pcs.health;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.boot.health.contributor.Status;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.core.env.StandardEnvironment;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class IdamHealthConfigurationTest {

    // Ignore environment variables: CI sets IDAM_API_URL, which binds to idam.api.url.
    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
        .withInitializer(context -> context.getEnvironment().getPropertySources()
            .remove(StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME))
        .withUserConfiguration(IdamHealthConfiguration.class);

    private HttpServer idam;
    private volatile int statusCode;
    private volatile String body;

    @BeforeEach
    void startIdam() throws IOException {
        idam = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        idam.createContext("/health", exchange -> {
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(statusCode, bytes.length);
            try (OutputStream out = exchange.getResponseBody()) {
                out.write(bytes);
            }
        });
        idam.start();
    }

    @AfterEach
    void stopIdam() {
        idam.stop(0);
    }

    @Test
    void shouldRegisterIdamIndicatorWhenUrlIsSet() {
        contextRunner
            .withPropertyValues("idam.api.url=" + idamUrl())
            .run(context -> assertThat(context).hasBean("idamHealthIndicator"));
    }

    @Test
    void shouldNotRegisterIndicatorWithoutUrl() {
        contextRunner.run(context -> assertThat(context).doesNotHaveBean(HealthIndicator.class));
    }

    @Test
    void shouldReportIdamStatusAndIgnoreOtherFields() {
        respond(200, "{\"status\":\"UP\",\"components\":{\"db\":{\"status\":\"UP\"}}}");

        assertThat(idamStatus(idamUrl())).isEqualTo(Status.UP);
    }

    @Test
    void shouldReportDownWhenIdamReturnsAnError() {
        respond(503, "{\"status\":\"DOWN\"}");

        assertThat(idamStatus(idamUrl())).isEqualTo(Status.DOWN);
    }

    @Test
    void shouldReportDownWhenIdamIsUnreachable() {
        assertThat(idamStatus("http://localhost:1")).isEqualTo(Status.DOWN);
    }

    private Status idamStatus(String baseUrl) {
        return new IdamHealthConfiguration().idamHealthIndicator(baseUrl).health().getStatus();
    }

    private void respond(int statusCode, String body) {
        this.statusCode = statusCode;
        this.body = body;
    }

    private String idamUrl() {
        return "http://localhost:" + idam.getAddress().getPort();
    }
}
