package uk.gov.hmcts.reform.pcs.notify.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import uk.gov.hmcts.reform.pcs.notify.entities.CaseNotification;

import java.util.Optional;
import java.util.UUID;

public interface NotificationRepository extends JpaRepository<CaseNotification, UUID> {
    /**
     * Find a case notification by its provider notification ID.
     *
     * @param providerNotificationId The provider notification ID to search for
     * @return An Optional containing the CaseNotification if found
     */
    Optional<CaseNotification> findByProviderNotificationId(UUID providerNotificationId);
}
