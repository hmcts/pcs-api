package uk.gov.hmcts.reform.pcs.ccd.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import uk.gov.hmcts.reform.pcs.ccd.entity.RefDataFlagsEntity;

import java.util.Optional;
import java.util.UUID;

public interface RefDataFlagsRepository extends JpaRepository<RefDataFlagsEntity, UUID> {

    Optional<RefDataFlagsEntity> findByFlagCode(String flagCode);
}
