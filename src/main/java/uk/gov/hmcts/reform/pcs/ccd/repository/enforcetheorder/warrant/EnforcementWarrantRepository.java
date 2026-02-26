package uk.gov.hmcts.reform.pcs.ccd.repository.enforcetheorder.warrant;

import org.springframework.data.jpa.repository.JpaRepository;
import uk.gov.hmcts.reform.pcs.ccd.entity.enforcetheorder.warrant.EnforcementWarrantEntity;

import java.util.UUID;

public interface EnforcementWarrantRepository extends JpaRepository<EnforcementWarrantEntity, UUID> {
}
