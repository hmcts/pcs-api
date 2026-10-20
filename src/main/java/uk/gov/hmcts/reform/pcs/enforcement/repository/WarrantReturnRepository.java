package uk.gov.hmcts.reform.pcs.enforcement.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import uk.gov.hmcts.reform.pcs.enforcement.entity.WarrantReturnEntity;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface WarrantReturnRepository extends JpaRepository<WarrantReturnEntity, UUID> {

    List<WarrantReturnEntity> findByEnforcementOrderIdOrderByRecordedAtDesc(UUID enforcementOrderId);

    Optional<WarrantReturnEntity> findFirstByEnforcementOrderIdOrderByRecordedAtDesc(UUID enforcementOrderId);

    List<WarrantReturnEntity> findByEnforcementOrderClaimIdOrderByRecordedAtDesc(UUID claimId);
}
