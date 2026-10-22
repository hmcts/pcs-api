package uk.gov.hmcts.reform.pcs.enforcement.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import uk.gov.hmcts.reform.pcs.enforcement.entity.BailiffAllocationEntity;

import java.util.List;
import java.util.UUID;

public interface BailiffAllocationRepository extends JpaRepository<BailiffAllocationEntity, UUID> {

    /**
     * Returns at most one row: a unique constraint allows only one active allocation per enforcement.
     */
    List<BailiffAllocationEntity> findByEnforcementOrderIdAndEndedAtIsNull(UUID enforcementOrderId);

    List<BailiffAllocationEntity> findByEnforcementOrderIdOrderByStartedAtAsc(UUID enforcementOrderId);
}
