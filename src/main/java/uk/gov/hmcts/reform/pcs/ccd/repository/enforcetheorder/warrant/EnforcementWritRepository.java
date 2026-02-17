package uk.gov.hmcts.reform.pcs.ccd.repository.enforcetheorder.warrant;

import org.springframework.data.jpa.repository.JpaRepository;
import uk.gov.hmcts.reform.pcs.ccd.entity.enforcetheorder.writ.EnforcementWritEntity;

import java.util.UUID;

public interface EnforcementWritRepository extends JpaRepository<EnforcementWritEntity, UUID> {
}
