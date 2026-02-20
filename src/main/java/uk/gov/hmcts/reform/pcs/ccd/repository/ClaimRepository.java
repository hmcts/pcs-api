package uk.gov.hmcts.reform.pcs.ccd.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimEntity;

import java.util.Optional;
import java.util.UUID;


@Repository
public interface ClaimRepository extends JpaRepository<ClaimEntity, UUID> {

    @Query("SELECT c.id FROM ClaimEntity c WHERE c.pcsCase.caseReference = :caseReference")
    Optional<UUID> findIdByCaseReference(@Param("caseReference") long caseReference);

}
