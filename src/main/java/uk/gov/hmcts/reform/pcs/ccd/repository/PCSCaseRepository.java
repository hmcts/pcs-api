package uk.gov.hmcts.reform.pcs.ccd.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import uk.gov.hmcts.reform.pcs.ccd.entity.PCS;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface PCSCaseRepository extends JpaRepository<PCS, UUID> {

    Optional<PCS> findByCaseReference(Long caseReference);
}
