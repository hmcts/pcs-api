package uk.gov.hmcts.reform.pcs.ccd.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import uk.gov.hmcts.reform.pcs.ccd.domain.claimactivitylog.ClaimActivityStatus;
import uk.gov.hmcts.reform.pcs.ccd.domain.claimactivitylog.ClaimActivityType;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimActivityLogEntity;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface ClaimActivityLogRepository extends JpaRepository<ClaimActivityLogEntity, Long> {

    List<ClaimActivityLogEntity> findAllByPcsCase_Id(UUID caseId);

    @Query("select distinct log.pcsCase.id from ClaimActivityLogEntity log "
        + "where log.activityType = :activityType and log.status = :status")
    List<UUID> findCaseIdsByActivityTypeAndStatus(@Param("activityType") ClaimActivityType activityType,
                                                  @Param("status") ClaimActivityStatus status);

    @Query("select distinct log.pcsCase.id from ClaimActivityLogEntity log "
        + "where log.activityType = :activityType and log.status = :status and log.createdAt >= :createdAfter")
    List<UUID> findCaseIdsByActivityTypeAndStatusCreatedAfter(@Param("activityType") ClaimActivityType activityType,
                                                              @Param("status") ClaimActivityStatus status,
                                                              @Param("createdAfter") LocalDateTime createdAfter);

}

