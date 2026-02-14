package uk.gov.hmcts.reform.pcs.ccd.repository.enforcetheorder.warrant;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import uk.gov.hmcts.reform.pcs.ccd.entity.enforcetheorder.warrant.EnforcementSelectedDefendantEntity;

import java.util.UUID;

@Repository
public interface EnforcementSelectedDefendantRepository extends
    JpaRepository<EnforcementSelectedDefendantEntity, UUID> {
}
