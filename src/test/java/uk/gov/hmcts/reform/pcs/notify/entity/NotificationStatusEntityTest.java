package uk.gov.hmcts.reform.pcs.notify.entity;

import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;
import static org.assertj.core.api.Assertions.assertThat;

class NotificationStatusEntityTest {

    @Test
    void testEntityCreation() {
        // Given
        String notificationId = "test-notification-id";
        String status = "delivered";
        LocalDateTime now = LocalDateTime.now();

        // When
        NotificationStatusEntity entity = new NotificationStatusEntity(
            notificationId,
            status,
            now,
            now
        );

        // Then
        assertThat(entity.getNotificationId()).isEqualTo(notificationId);
        assertThat(entity.getStatus()).isEqualTo(status);
        assertThat(entity.getCreatedAt()).isEqualTo(now);
        assertThat(entity.getLastUpdated()).isEqualTo(now);
    }
}