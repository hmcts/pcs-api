package uk.gov.hmcts.reform.pcs.ccd.repository.enforcetheorder;

import org.springframework.data.jpa.repository.JpaRepository;
import uk.gov.hmcts.reform.pcs.ccd.entity.enforcetheorder.ScheduledEvictionEntity;

public interface ScheduleEvictionRepository extends JpaRepository<ScheduledEvictionEntity, Integer> {
}
