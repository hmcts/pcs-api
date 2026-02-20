package uk.gov.hmcts.reform.pcs.ccd.repository.enforcetheorder.warrant;

import org.springframework.data.jpa.repository.JpaRepository;
import uk.gov.hmcts.reform.pcs.ccd.entity.enforcetheorder.warrant.RiskProfileEntity;

import java.util.UUID;

public interface RiskProfileRepository
        extends JpaRepository<RiskProfileEntity, UUID> {
}
