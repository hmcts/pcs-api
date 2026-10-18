package uk.gov.hmcts.reform.pcs.enforcement.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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

    /**
     * Every active allocation to the bailiff, across all cases. {@code bailiffUserId} must already be lower case:
     * IDAM ids are compared case-insensitively, as {@link uk.gov.hmcts.reform.pcs.idam.IdamUserIds} normalises them.
     */
    @Query("select allocation from BailiffAllocationEntity allocation "
        + "where lower(allocation.bailiffUserId) = :bailiffUserId and allocation.endedAt is null")
    List<BailiffAllocationEntity> findActiveByBailiffUserId(@Param("bailiffUserId") String bailiffUserId);
}
