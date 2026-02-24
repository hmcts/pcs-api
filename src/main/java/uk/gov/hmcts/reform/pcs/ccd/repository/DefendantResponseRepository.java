package uk.gov.hmcts.reform.pcs.ccd.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import uk.gov.hmcts.reform.pcs.ccd.entity.DefendantResponseEntity;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface DefendantResponseRepository extends JpaRepository<DefendantResponseEntity, UUID> {

    Optional<DefendantResponseEntity> findByClaimPcsCaseCaseReferenceAndPartyIdamId(
        Long caseReference, UUID partyIdamId);

    /**
     * Checks if a defendant response exists for the given case reference and party IDAM ID.
     * More efficient than findBy...() as it only checks existence without loading the entity.
     *
     * @param caseReference The case reference number
     * @param partyIdamId The party's IDAM user ID
     * @return true if a response exists, false otherwise
     */
    boolean existsByClaimPcsCaseCaseReferenceAndPartyIdamId(Long caseReference, UUID partyIdamId);
}
