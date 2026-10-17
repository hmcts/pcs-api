package uk.gov.hmcts.reform.pcs.enforcement.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import uk.gov.hmcts.reform.pcs.enforcement.entity.BailiffAllocationEntity;

import java.util.List;
import java.util.UUID;

public interface BailiffAllocationRepository extends JpaRepository<BailiffAllocationEntity, UUID> {

    /**
     * The enforcement's active allocations: at most one, as the database enforces.
     */
    List<BailiffAllocationEntity> findByEnforcementOrderIdAndEndedAtIsNull(UUID enforcementOrderId);

    /**
     * Every allocation of the enforcement, oldest first.
     */
    List<BailiffAllocationEntity> findByEnforcementOrderIdOrderByStartedAtAsc(UUID enforcementOrderId);
}
