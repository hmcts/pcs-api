package uk.gov.hmcts.reform.pcs.ccd.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimEntity;

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
        SELECT c.id FROM ClaimEntity c
        LEFT JOIN SdpClaimSnapshotEntity s ON s.claimId = c.id
        LEFT JOIN c.pcsCase pc
        LEFT JOIN pc.tenancyLicence tl
        LEFT JOIN c.noticeOfPossession n
        WHERE s.claimId IS NULL
           OR c.lastModified > s.exportedAt
           OR pc.lastModifiedDatetime > s.exportedAt
           OR tl.lastModified > s.exportedAt
           OR n.lastModified > s.exportedAt
           OR EXISTS (
                SELECT 1 FROM FeePaymentEntity fp
                WHERE fp.claim = c AND fp.lastModified > s.exportedAt
           )
           OR EXISTS (
                SELECT 1 FROM ClaimPartyEntity cp
                WHERE cp.claim = c AND cp.lastModified > s.exportedAt
           )
           OR EXISTS (
                SELECT 1 FROM ClaimPartyOrganisationEntity o, ClaimPartyEntity cp
                WHERE o.party = cp.party AND cp.claim = c AND o.lastModified > s.exportedAt
           )
           OR EXISTS (
                SELECT 1 FROM ClaimGroundEntity g
                WHERE g.claim = c AND g.lastModified > s.exportedAt
           )
           OR EXISTS (
                SELECT 1 FROM DefendantResponseEntity r
                WHERE r.claim = c AND r.lastModified > s.exportedAt
           )
        ORDER BY c.id
        """)
    List<UUID> findAllIdsForSdpSnapshotExport();
}
