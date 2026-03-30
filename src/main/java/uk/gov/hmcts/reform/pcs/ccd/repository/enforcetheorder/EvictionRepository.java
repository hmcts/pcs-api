package uk.gov.hmcts.reform.pcs.ccd.repository.enforcetheorder;

import org.springframework.data.jpa.repository.JpaRepository;
import uk.gov.hmcts.reform.pcs.ccd.entity.confirmeviction.EvictionEntity;

import java.util.UUID;

public interface EvictionRepository extends JpaRepository<EvictionEntity, UUID> {
}
