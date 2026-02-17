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
}
