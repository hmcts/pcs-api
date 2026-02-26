package uk.gov.hmcts.reform.pcs.ccd.repository.enforcetheorder.writofrestitution;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import uk.gov.hmcts.reform.pcs.ccd.entity.enforcetheorder.writofrestitution.WritOfRestitutionEntity;

import java.util.UUID;

@Repository
public interface WritOfRestitutionRepository extends
    JpaRepository<WritOfRestitutionEntity, UUID> {
}
