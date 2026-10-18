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

    /**
     * The warrants actively allocated to the bailiff whose status is one of {@code statuses}, in one query. A
     * warrant is an enforcement with warrant of possession or warrant of restitution details, as
     * {@link uk.gov.hmcts.reform.pcs.enforcement.service.BailiffEnforcementRules#isWarrant} decides.
     * {@code bailiffUserId} must already be lower case, as for {@link #findActiveByBailiffUserId}.
     */
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
