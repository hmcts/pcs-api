package uk.gov.hmcts.reform.pcs.notify.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import uk.gov.hmcts.reform.pcs.notify.entity.NotificationStatusEntity;

@Repository
public interface NotificationStatusRepository extends JpaRepository<NotificationStatusEntity, String> {
}