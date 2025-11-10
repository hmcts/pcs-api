package uk.gov.hmcts.reform.pcs.ccd.repository.enforcement;

import org.springframework.data.jpa.repository.JpaRepository;
import uk.gov.hmcts.reform.pcs.ccd.entity.enforcement.EnforcementDataEntity;

import java.util.Optional;
import java.util.UUID;

public interface EnforcementDataRepository extends JpaRepository<EnforcementDataEntity, UUID> {

    Optional<EnforcementDataEntity> findById(UUID enforcementCaseId);

    boolean existsById(UUID enforcementCaseId);

    void deleteById(UUID enforcementCaseId);
}
