package uk.gov.hmcts.reform.pcs.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import uk.gov.hmcts.reform.pcs.entity.PcsCase;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface PCSCaseRepository extends JpaRepository<PcsCase, UUID>, CustomPcsCaseRepository {

    Optional<PcsCase> findByCaseReference(long caseReference);

}
