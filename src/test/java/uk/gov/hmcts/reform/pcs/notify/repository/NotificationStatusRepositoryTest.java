package uk.gov.hmcts.reform.pcs.notify.repository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import uk.gov.hmcts.reform.pcs.notify.entity.NotificationStatusEntity;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(SpringExtension.class)
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class NotificationStatusRepositoryTest {

    @Autowired
    private NotificationStatusRepository repository;

    @Test
    void shouldSaveAndRetrieveNotificationStatus() {
        // Given
        String notificationId = "test-notification-id";
        LocalDateTime now = LocalDateTime.now();
        NotificationStatusEntity entity = new NotificationStatusEntity(
            notificationId,
            "sent",
            now,
            now
        );

        // When
        repository.save(entity);

        // Then
        Optional<NotificationStatusEntity> found = repository.findById(notificationId);
        assertThat(found).isPresent();
        assertThat(found.get().getStatus()).isEqualTo("sent");
    }

    @Test
    void shouldUpdateExistingNotificationStatus() {
        // Given
        String notificationId = "test-notification-id";
        LocalDateTime now = LocalDateTime.now();
        NotificationStatusEntity entity = new NotificationStatusEntity(
            notificationId,
            "sent",
            now,
            now
        );
        repository.save(entity);

        // When
        entity.setStatus("delivered");
        entity.setLastUpdated(now.plusMinutes(5));
        repository.save(entity);

        // Then
        Optional<NotificationStatusEntity> found = repository.findById(notificationId);
        assertThat(found).isPresent();
        assertThat(found.get().getStatus()).isEqualTo("delivered");
        assertThat(found.get().getLastUpdated()).isAfter(found.get().getCreatedAt());
    }
}