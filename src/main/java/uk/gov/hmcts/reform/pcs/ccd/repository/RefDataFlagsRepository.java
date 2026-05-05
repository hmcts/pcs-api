package uk.gov.hmcts.reform.pcs.ccd.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import uk.gov.hmcts.reform.pcs.ccd.entity.RefDataFlagEntity;

import java.util.Optional;
import java.util.UUID;

public interface RefDataFlagsRepository extends JpaRepository<RefDataFlagEntity, UUID> {

    Optional<RefDataFlagEntity> findByFlagCode(String flagCode);
}
