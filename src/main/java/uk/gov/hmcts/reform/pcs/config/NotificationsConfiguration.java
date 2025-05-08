package uk.gov.hmcts.reform.pcs.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import uk.gov.service.notify.NotificationClient;

@Configuration
public class NotificationsConfiguration {

    @Value("${GOV_NOTIFY_API_KEY:}")
    private String apiKey;

    @Value("${notify.status-check-delay-millis:60000}")
    private long statusCheckDelay;

    @Value("${notify.max-status-check-retries:5}")
    private int maxStatusCheckRetries;

    @Bean
    public NotificationClient notificationClient() {
        return new NotificationClient(apiKey);
    }

    @Bean
    public long statusCheckDelay() {
        return statusCheckDelay;
    }

    @Bean
    public int maxStatusCheckRetries() {
        return maxStatusCheckRetries;
    }
}
