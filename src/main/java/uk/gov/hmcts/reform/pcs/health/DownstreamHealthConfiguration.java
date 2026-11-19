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
import uk.gov.hmcts.reform.ccd.client.CoreCaseDataApi;
import uk.gov.hmcts.reform.ccd.client.healthcheck.CoreCaseDataHealthIndicator;

import java.time.Duration;

/**
 * Health checks for the services pcs-api calls. The payments and send-letter client libraries ship Spring Boot 3
 * health indicators that Spring Boot 4 can't load, and the CCD client's auto-configuration is excluded in
 * Application, so we register all three here. The bean names match the old ones, so /health still reports
 * coreCaseData, payments and sendLetter.
 */
@Slf4j
@Configuration(proxyBeanMethods = false)
public class DownstreamHealthConfiguration {

    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(2);
    private static final Duration READ_TIMEOUT = Duration.ofSeconds(5);

    @Bean
    @ConditionalOnProperty(prefix = "core_case_data", name = "api.url")
    public CoreCaseDataHealthIndicator coreCaseData(CoreCaseDataApi coreCaseDataApi) {
        return new CoreCaseDataHealthIndicator(coreCaseDataApi);
    }

    @Bean
    @ConditionalOnProperty(prefix = "payments", name = "api.url")
    public HealthIndicator paymentsHealthIndicator(@Value("${payments.api.url}") String baseUrl) {
        return healthCheck("payments", baseUrl);
    }

    @Bean
    @ConditionalOnProperty(prefix = "send-letter", name = "url")
    public HealthIndicator sendLetterHealthIndicator(@Value("${send-letter.url}") String baseUrl) {
        return healthCheck("send-letter", baseUrl);
    }

    private static HealthIndicator healthCheck(String service, String baseUrl) {
        SimpleClientHttpRequestFactory timeouts = new SimpleClientHttpRequestFactory();
        timeouts.setConnectTimeout(CONNECT_TIMEOUT);
        timeouts.setReadTimeout(READ_TIMEOUT);
        RestClient client = RestClient.builder().baseUrl(baseUrl).requestFactory(timeouts).build();

        return () -> {
            try {
                HealthResponse response = client.get().uri("/health").retrieve().body(HealthResponse.class);
                return Health.status(response.status()).build();
            } catch (Exception ex) {
                log.error("{} health check failed", service, ex);
                return Health.down(ex).build();
            }
        };
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record HealthResponse(String status) {
    }
}
