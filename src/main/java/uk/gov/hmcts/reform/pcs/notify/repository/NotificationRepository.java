package uk.gov.hmcts.reform.pcs.notify.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import uk.gov.hmcts.reform.pcs.notify.entities.CaseNotification;

import java.util.Optional;
import java.util.UUID;

public interface NotificationRepository extends JpaRepository<CaseNotification, Integer> {
    /**
     * Find a case notification by its provider notification ID.
     *
     * @param providerNotificationId The provider notification ID to search for
     * @return An Optional containing the CaseNotification if found
     */
    Optional<CaseNotification> findByProviderNotificationId(UUID providerNotificationId);

    /**
     * The reference of the case the notification is about.
     */
    @Query("SELECT n.pcsCase.caseReference FROM CaseNotification n WHERE n.id = :id")
    Optional<Long> findCaseReferenceById(@Param("id") Integer id);
}
