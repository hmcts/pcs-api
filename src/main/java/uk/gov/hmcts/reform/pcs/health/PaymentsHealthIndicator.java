package uk.gov.hmcts.reform.pcs.health;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.stereotype.Component;

/**
 * Replaces the health indicator from payments-java-client, which is built on Spring Boot 3.
 * Same bean name, so /health still reports it as "payments".
 */
@Slf4j
@Component("paymentsHealthIndicator")
@ConditionalOnProperty(prefix = "payments", name = "api.url")
public class PaymentsHealthIndicator implements HealthIndicator {

    private final PaymentsHealthApi healthApi;

    public PaymentsHealthIndicator(PaymentsHealthApi healthApi) {
        this.healthApi = healthApi;
    }

    @Override
    public Health health() {
        try {
            return healthApi.health().toHealth();
        } catch (Exception ex) {
            log.error("Payments health check failed", ex);
            return Health.down(ex).build();
        }
    }
}
