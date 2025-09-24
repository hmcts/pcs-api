package uk.gov.hmcts.reform.pcs.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import uk.gov.service.notify.NotificationClient;

@Configuration
public class NotificationsConfiguration {
    @Bean
    public NotificationClient notificationClient(
        @Value("${notify.api-key}") String apiKey
    ) {
        return new NotificationClient(apiKey);
    }
}
