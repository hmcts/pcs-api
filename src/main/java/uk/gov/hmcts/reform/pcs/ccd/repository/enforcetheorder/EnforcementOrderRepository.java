package uk.gov.hmcts.reform.pcs.ccd.repository.enforcetheorder;

import org.springframework.data.jpa.repository.JpaRepository;
import uk.gov.hmcts.reform.pcs.ccd.entity.enforcetheorder.EnforcementOrderEntity;

import java.util.UUID;

public interface EnforcementOrderRepository extends JpaRepository<EnforcementOrderEntity, UUID> {
}
