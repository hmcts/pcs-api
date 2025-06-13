package uk.gov.hmcts.reform.pcs.ccd.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;

import java.util.Optional;

public interface PcsCaseRepository extends JpaRepository<PcsCaseEntity, Long> {

    Optional<PcsCaseEntity> findByCaseReference(long caseReference);

}
