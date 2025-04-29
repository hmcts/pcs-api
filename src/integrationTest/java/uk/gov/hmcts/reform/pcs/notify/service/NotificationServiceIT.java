package uk.gov.hmcts.reform.pcs.notify.service;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import uk.gov.hmcts.reform.pcs.Application;
import uk.gov.hmcts.reform.pcs.config.AbstractPostgresContainerIT;
import uk.gov.hmcts.reform.pcs.notify.entity.NotificationStatusEntity;
import uk.gov.hmcts.reform.pcs.notify.model.EmailNotificationRequest;
import uk.gov.hmcts.reform.pcs.notify.repository.NotificationStatusRepository;
import uk.gov.service.notify.SendEmailResponse;

import java.util.HashMap;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@Slf4j
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@SpringBootTest(classes = Application.class)
@ActiveProfiles("integration")
class NotificationServiceIT extends AbstractPostgresContainerIT {

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private NotificationStatusRepository repository;

    @Test
    void shouldPersistNotificationStatusThroughEntireFlow() throws Exception {
        // Given
        EmailNotificationRequest request = EmailNotificationRequest.builder()
            .templateId("template-id")
            .emailAddress("test@example.com")
            .personalisation(new HashMap<>())
            .reference("test-reference-" + UUID.randomUUID())
            .build();

        // When
        SendEmailResponse response = notificationService.sendEmail(request);
        String notificationId = response.getNotificationId().toString();

        // Then - verify initial status is saved
        await()
            .atMost(5, TimeUnit.SECONDS)
            .untilAsserted(() -> {
                Optional<NotificationStatusEntity> status = repository.findById(notificationId);
                assertThat(status)
                    .isPresent()
                    .hasValueSatisfying(s -> 
                        assertThat(s.getStatus()).isNotNull()
                    );
            });

        // When - check status
        notificationService.checkNotificationStatus(notificationId);

        // Then - verify status is updated
        await()
            .atMost(5, TimeUnit.SECONDS)
            .untilAsserted(() -> {
                Optional<NotificationStatusEntity> status = repository.findById(notificationId);
                assertThat(status)
                    .isPresent()
                    .hasValueSatisfying(s -> {
                        assertThat(s.getLastUpdated()).isAfter(s.getCreatedAt());
                    });
            });
    }
}