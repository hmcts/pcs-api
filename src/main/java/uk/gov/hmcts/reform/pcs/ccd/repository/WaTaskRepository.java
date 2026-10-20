package uk.gov.hmcts.reform.pcs.ccd.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import uk.gov.hmcts.reform.pcs.ccd.entity.WaTaskEntity;

public interface WaTaskRepository extends JpaRepository<WaTaskEntity, Integer> {
}
