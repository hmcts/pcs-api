package uk.gov.hmcts.reform.pcs.ccd.repository;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;

import java.util.Optional;
import java.util.UUID;

public interface PcsCaseRepository extends JpaRepository<PcsCaseEntity, UUID> {

    @EntityGraph(value = "PcsCaseEntity.parties")
    Optional<PcsCaseEntity> findByCaseReference(long caseReference);

}
