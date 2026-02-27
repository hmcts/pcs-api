package uk.gov.hmcts.reform.pcs.ccd.repository.enforcetheorder.warrantofrestitution;

import org.springframework.data.jpa.repository.JpaRepository;
import uk.gov.hmcts.reform.pcs.ccd.entity.enforcetheorder.warrantofrestitution.WarrantOfRestitutionEntity;

import java.util.UUID;

public interface WarrantOfRestitutionRepository extends JpaRepository<WarrantOfRestitutionEntity, UUID> {
}
