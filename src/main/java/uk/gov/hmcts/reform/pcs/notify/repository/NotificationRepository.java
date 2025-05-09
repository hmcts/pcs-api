package uk.gov.hmcts.reform.pcs.notify.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import uk.gov.hmcts.reform.pcs.notify.domain.CaseNotification;

import java.util.Optional;
import java.util.UUID;

public interface NotificationRepository extends JpaRepository<CaseNotification, UUID> {
    Optional<CaseNotification> findByProviderNotificationId(UUID providerNotificationId);
}
