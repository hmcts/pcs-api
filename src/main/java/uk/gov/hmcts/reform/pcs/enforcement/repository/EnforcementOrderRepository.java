package uk.gov.hmcts.reform.pcs.enforcement.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import uk.gov.hmcts.reform.pcs.enforcement.domain.EnforcementStatus;
import uk.gov.hmcts.reform.pcs.enforcement.entity.EnforcementOrderEntity;

import java.util.Collection;
import java.util.List;
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
     * The next value of enf_case_number_seq, from which warrant and writ numbers are built.
     */
    @Query(value = "SELECT nextval('enf_case_number_seq')", nativeQuery = true)
    long nextEnforcementNumberSequenceValue();
}
