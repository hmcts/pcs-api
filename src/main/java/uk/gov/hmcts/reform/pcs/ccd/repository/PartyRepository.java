package uk.gov.hmcts.reform.pcs.ccd.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyRole;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PartyRepository extends JpaRepository<PartyEntity, UUID> {

    @Query("SELECT p FROM PartyEntity p WHERE p.id = :id AND p.pcsCase.caseReference = :caseReference")
    Optional<PartyEntity> queryPartyById(@Param("id") UUID id, @Param("caseReference") long caseReference);

    @Query("SELECT p FROM PartyEntity p WHERE p.idamId = :idamId AND p.pcsCase.caseReference = :caseReference")
    Optional<PartyEntity> queryPartyByIdamId(@Param("idamId") UUID idamId, @Param("caseReference") long caseReference);

    @Query("SELECT cp.claim FROM ClaimPartyEntity cp WHERE cp.party.idamId = :idamId AND cp.role = :role")
    List<ClaimEntity> findClaimsByIdamIdAndRole(@Param("idamId") UUID idamId, @Param("role") PartyRole role);

}
