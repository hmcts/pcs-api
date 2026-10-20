package uk.gov.hmcts.reform.pcs.enforcement.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import uk.gov.hmcts.reform.pcs.enforcement.domain.EnforcementStatus;
import uk.gov.hmcts.reform.pcs.enforcement.entity.BailiffAllocationEntity;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface BailiffAllocationRepository extends JpaRepository<BailiffAllocationEntity, UUID> {

    /**
     * Returns at most one row: a unique constraint allows only one active allocation per enforcement.
     */
    List<BailiffAllocationEntity> findByEnforcementOrderIdAndEndedAtIsNull(UUID enforcementOrderId);

    List<BailiffAllocationEntity> findByEnforcementOrderIdOrderByStartedAtAsc(UUID enforcementOrderId);

    /**
     * {@code bailiffUserId} must already be lower case.
     */
    @Query("select allocation from BailiffAllocationEntity allocation "
        + "where lower(allocation.bailiffUserId) = :bailiffUserId and allocation.endedAt is null")
    List<BailiffAllocationEntity> findActiveByBailiffUserId(@Param("bailiffUserId") String bailiffUserId);

    @Query("select new uk.gov.hmcts.reform.pcs.enforcement.repository.AllocatedWarrantRow("
        + "pcsCase.caseReference, enforcement.enforcementNumber, enforcement.status, "
        + "enforcement.bailiffDate) "
        + "from BailiffAllocationEntity allocation "
        + "join allocation.enforcementOrder enforcement "
        + "join enforcement.claim claim "
        + "join claim.pcsCase pcsCase "
        + "where lower(allocation.bailiffUserId) = :bailiffUserId and allocation.endedAt is null "
        + "and enforcement.status in :statuses "
        + "and (exists (select 1 from WarrantEntity warrant where warrant.enforcementOrder = enforcement) "
        + "or exists (select 1 from WarrantOfRestitutionEntity restitution "
        + "where restitution.enforcementOrder = enforcement))")
    List<AllocatedWarrantRow> findAllocatedWarrants(@Param("bailiffUserId") String bailiffUserId,
                                                    @Param("statuses") Collection<EnforcementStatus> statuses);
}
