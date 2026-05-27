package uk.gov.hmcts.reform.pcs.ccd.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import uk.gov.hmcts.reform.pcs.ccd.entity.FlagRefDataEntity;

import java.util.Optional;
import java.util.UUID;

public interface FlagRefDataRepository extends JpaRepository<FlagRefDataEntity, UUID> {

    Optional<FlagRefDataEntity> findByFlagCode(String flagCode);
}
