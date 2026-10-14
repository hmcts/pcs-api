package uk.gov.hmcts.reform.pcs.ccd.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimEntity;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;


@Repository
public interface ClaimRepository extends JpaRepository<ClaimEntity, UUID> {

    @Query("SELECT c.id FROM ClaimEntity c WHERE c.pcsCase.caseReference = :caseReference")
    Optional<UUID> findIdByCaseReference(@Param("caseReference") long caseReference);

    @Query("SELECT c FROM ClaimEntity c WHERE c.pcsCase.caseReference = :caseReference")
    Optional<ClaimEntity> findClaimByCaseReference(@Param("caseReference") long caseReference);

    @Query("""
        SELECT DISTINCT c FROM ClaimEntity c
        JOIN FETCH c.pcsCase pc
        LEFT JOIN FETCH pc.tenancyLicence
        LEFT JOIN FETCH c.noticeOfPossession
        """)
    List<ClaimEntity> findAllForSdpSnapshotExport();

    @Query("""
        SELECT DISTINCT c FROM ClaimEntity c
        JOIN FETCH c.pcsCase pc
        LEFT JOIN FETCH pc.tenancyLicence
        LEFT JOIN FETCH c.noticeOfPossession
        WHERE c.claimSubmittedDate >= :from
        AND c.claimSubmittedDate < :to
        """)
    List<ClaimEntity> findForSdpSnapshotExportBetween(@Param("from") LocalDateTime from,
                                                      @Param("to") LocalDateTime to);
}
