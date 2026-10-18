package uk.gov.hmcts.reform.pcs.enforcement.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import uk.gov.hmcts.reform.pcs.enforcement.domain.EnforcementStatus;
import uk.gov.hmcts.reform.pcs.enforcement.entity.EnforcementOrderEntity;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EnforcementOrderRepository extends JpaRepository<EnforcementOrderEntity, UUID> {
    List<EnforcementOrderEntity> findByClaimId(UUID claimId);

    /**
     * Whether the claim has an enforcement in any status other than {@code statuses}. Rows with no status
     * never match.
     */
    boolean existsByClaimIdAndStatusNotIn(UUID claimId, Collection<EnforcementStatus> statuses);

    List<EnforcementOrderEntity> findByClaimIdAndStatus(UUID claimId, EnforcementStatus status);

    /**
     * Every warrant and writ number given on the case, oldest approval first.
     */
    @Query("SELECT e.enforcementNumber FROM EnforcementOrderEntity e "
        + "WHERE e.claim.pcsCase.caseReference = :caseReference AND e.enforcementNumber IS NOT NULL "
        + "ORDER BY e.approvedAt, e.enforcementNumber")
    List<String> findEnforcementNumbersByCaseReference(@Param("caseReference") long caseReference);

    /**
     * The reference of the case the enforcement is on.
     */
    @Query("SELECT e.claim.pcsCase.caseReference FROM EnforcementOrderEntity e WHERE e.id = :id")
    Optional<Long> findCaseReferenceById(@Param("id") UUID id);

    /**
     * The next value of enf_case_number_seq, from which warrant and writ numbers are built.
     */
    @Query(value = "SELECT nextval('enf_case_number_seq')", nativeQuery = true)
    long nextEnforcementNumberSequenceValue();
}
