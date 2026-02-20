package uk.gov.hmcts.reform.pcs.ccd.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface PartyRepository extends JpaRepository<PartyEntity, UUID> {
    Optional<PartyEntity> findByIdamId(UUID idamId);

    @Query("SELECT p.id FROM PartyEntity p WHERE p.idamId = :idamId")
    Optional<UUID> findIdByIdamId(@Param("idamId") UUID idamId);
}
