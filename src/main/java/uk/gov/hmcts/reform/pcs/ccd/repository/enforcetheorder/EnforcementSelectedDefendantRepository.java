package uk.gov.hmcts.reform.pcs.ccd.repository.enforcetheorder;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import uk.gov.hmcts.reform.pcs.ccd.entity.enforcetheorder.EnforcementSelectedDefendantEntity;

import java.util.UUID;

@Repository
public interface EnforcementSelectedDefendantRepository extends
    JpaRepository<EnforcementSelectedDefendantEntity, UUID> {
}
