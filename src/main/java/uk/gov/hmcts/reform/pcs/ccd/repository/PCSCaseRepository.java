package uk.gov.hmcts.reform.pcs.ccd.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import uk.gov.hmcts.reform.pcs.ccd.entity.PCSCaseEntity;

import java.util.Optional;
import java.util.UUID;

public interface PCSCaseRepository extends JpaRepository<PCSCaseEntity, UUID> {

    Optional<PCSCaseEntity> findByCaseReference(long caseReference);

}
