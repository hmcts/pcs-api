package uk.gov.hmcts.reform.pcs.ccd.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimActivityLogEntity;

import java.util.List;
import java.util.UUID;

@Repository
public interface ClaimActivityLogRepository extends JpaRepository<ClaimActivityLogEntity, UUID> {

    List<ClaimActivityLogEntity> findAllByPcsCase_Id(UUID caseId);

}

