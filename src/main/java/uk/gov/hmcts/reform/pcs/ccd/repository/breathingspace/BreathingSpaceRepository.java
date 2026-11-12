package uk.gov.hmcts.reform.pcs.ccd.repository.breathingspace;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import uk.gov.hmcts.reform.pcs.ccd.entity.breathingspace.BreathingSpaceEntity;

import java.util.List;
import java.util.UUID;

public interface BreathingSpaceRepository extends JpaRepository<BreathingSpaceEntity, UUID> {

    @Query("""
        SELECT bs
        FROM BreathingSpaceEntity bs
        WHERE bs.claim.pcsCase.caseReference = :caseReference
        """)
    List<BreathingSpaceEntity> findByCaseReference(
        @Param("caseReference") long caseReference
    );
}
