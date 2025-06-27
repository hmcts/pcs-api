package uk.gov.hmcts.reform.pcs.notify.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import uk.gov.hmcts.reform.pcs.notify.model.NotificationStatus;

import java.time.Instant;
import java.util.UUID;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "case_notification")
@Getter
@Setter
@ToString
@EqualsAndHashCode(of = "notificationId")
public class CaseNotification {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "notification_id", nullable = false)
    private UUID notificationId;

    @Column(name = "case_id", nullable = false)
    private UUID caseId;

    @Column(name = "provider_notification_id", nullable = true)
    private UUID providerNotificationId;

    @Column(name = "submitted_at")
    private Instant submittedAt;

    @Column(name = "scheduled_at")
    private Instant scheduledAt;

    @Column(name = "last_updated_at", nullable = false)
    private Instant lastUpdatedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private NotificationStatus status;

    @Column(name = "type", nullable = false)
    private String type;

    @Column(name = "recipient", nullable = false)
    private String recipient;

    @PrePersist
    public void prePersist() {
        this.lastUpdatedAt = Instant.now();
    }

    @PreUpdate
    public void preUpdate() {
        this.lastUpdatedAt = Instant.now();
    }
}
