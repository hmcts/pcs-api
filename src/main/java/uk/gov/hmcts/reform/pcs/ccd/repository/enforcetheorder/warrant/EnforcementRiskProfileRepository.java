package uk.gov.hmcts.reform.pcs.ccd.repository.enforcetheorder.warrant;

import org.springframework.data.jpa.repository.JpaRepository;
import uk.gov.hmcts.reform.pcs.ccd.entity.enforcetheorder.warrant.EnforcementRiskProfileEntity;

import java.util.UUID;

public interface EnforcementRiskProfileRepository
        extends JpaRepository<EnforcementRiskProfileEntity, UUID> {
}
