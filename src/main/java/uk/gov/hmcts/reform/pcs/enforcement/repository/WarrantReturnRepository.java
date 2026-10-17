package uk.gov.hmcts.reform.pcs.enforcement.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import uk.gov.hmcts.reform.pcs.enforcement.entity.WarrantReturnEntity;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface WarrantReturnRepository extends JpaRepository<WarrantReturnEntity, UUID> {

    /**
     * The enforcement's returns, newest first.
     */
    List<WarrantReturnEntity> findByEnforcementOrderIdOrderByRecordedAtDesc(UUID enforcementOrderId);

    /**
     * The enforcement's most recent return.
     */
    Optional<WarrantReturnEntity> findFirstByEnforcementOrderIdOrderByRecordedAtDesc(UUID enforcementOrderId);

    /**
     * Every return on every enforcement of the claim, newest first.
     */
    List<WarrantReturnEntity> findByEnforcementOrderClaimIdOrderByRecordedAtDesc(UUID claimId);
}
