package uk.gov.hmcts.reform.pcs.notify.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "notification_status")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class NotificationStatusEntity {
    @Id
    private String notificationId;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime lastUpdated;
}