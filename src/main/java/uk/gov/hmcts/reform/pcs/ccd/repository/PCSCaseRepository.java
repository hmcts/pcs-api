package uk.gov.hmcts.reform.pcs.ccd.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;

import java.util.Optional;
import java.util.UUID;

public interface PCSCaseRepository extends JpaRepository<PcsCaseEntity, UUID> {

    Optional<PcsCaseEntity> findByCaseReference(long caseReference);

}
