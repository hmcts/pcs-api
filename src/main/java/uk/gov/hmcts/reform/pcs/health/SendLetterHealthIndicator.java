package uk.gov.hmcts.reform.pcs.health;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.stereotype.Component;

/**
 * Replaces the health indicator from send-letter-client, which is built on Spring Boot 3.
 * Same bean name, so /health still reports it as "sendLetter".
 */
@Slf4j
@Component("sendLetterHealthIndicator")
@ConditionalOnProperty(prefix = "send-letter", name = "url")
public class SendLetterHealthIndicator implements HealthIndicator {

    private final SendLetterHealthApi healthApi;

    public SendLetterHealthIndicator(SendLetterHealthApi healthApi) {
        this.healthApi = healthApi;
    }

    @Override
    public Health health() {
        try {
            return healthApi.health().toHealth();
        } catch (Exception ex) {
            log.error("Send letter health check failed", ex);
            return Health.down(ex).build();
        }
    }
}
