package uk.gov.hmcts.reform.pcs.health;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.time.Duration;

@Slf4j
@Configuration(proxyBeanMethods = false)
public class IdamHealthConfiguration {

    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(2);
    private static final Duration READ_TIMEOUT = Duration.ofSeconds(5);

    @Bean
    @ConditionalOnProperty(prefix = "idam", name = "api.url")
    public HealthIndicator idamHealthIndicator(@Value("${idam.api.url}") String baseUrl) {
        SimpleClientHttpRequestFactory timeouts = new SimpleClientHttpRequestFactory();
        timeouts.setConnectTimeout(CONNECT_TIMEOUT);
        timeouts.setReadTimeout(READ_TIMEOUT);
        RestClient client = RestClient.builder().baseUrl(baseUrl).requestFactory(timeouts).build();

        return () -> {
            try {
                HealthResponse response = client.get().uri("/health").retrieve().body(HealthResponse.class);
                return Health.status(response.status()).build();
            } catch (Exception ex) {
                log.error("IDAM health check failed", ex);
                return Health.down(ex).build();
            }
        };
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record HealthResponse(String status) {
    }
}
