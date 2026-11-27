package uk.gov.hmcts.reform.pcs.ccd.repository.breathingspace;

import org.springframework.data.jpa.repository.JpaRepository;
import uk.gov.hmcts.reform.pcs.ccd.entity.breathingspace.BreathingSpaceEntity;

import java.util.List;
import java.util.UUID;

public interface BreathingSpaceRepository extends JpaRepository<BreathingSpaceEntity, UUID> {


    List<BreathingSpaceEntity> findByPcsCase_CaseReferenceOrderByCreatedAtDescIdDesc(long caseReference);
}

