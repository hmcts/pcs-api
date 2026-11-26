package uk.gov.hmcts.reform.pcs.ccd.repository.enforcetheorder;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.EnforcementState;
import uk.gov.hmcts.reform.pcs.ccd.entity.enforcetheorder.EnforcementOrderEntity;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface EnforcementOrderRepository extends JpaRepository<EnforcementOrderEntity, UUID> {

    int deleteByLastModifiedAtBeforeAndStateIn(Instant cutoff, Collection<EnforcementState> states);

    @Query("""
        SELECT DISTINCT pcsCase.caseReference
        FROM EnforcementOrderEntity application
        JOIN application.pcsCase pcsCase
        WHERE application.state = :state
        """)
    List<Long> findCaseReferencesByState(@Param("state") EnforcementState state);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
        UPDATE EnforcementOrderEntity application
        SET application.state = :newState,
            application.lastModifiedAt = CURRENT_TIMESTAMP
        WHERE application.pcsCase.caseReference IN (:caseReferences)
          AND application.state = :currentState
        """)
    int updateStateByCaseReferenceInAndState(
        @Param("caseReferences") Collection<Long> caseReferences,
        @Param("currentState") EnforcementState currentState,
        @Param("newState") EnforcementState newState
    );
}
