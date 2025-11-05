package uk.gov.hmcts.reform.pcs.ccd.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import uk.gov.hmcts.reform.pcs.ccd.entity.DraftCaseDataEntity;

import java.util.Optional;
import java.util.UUID;

public interface DraftCaseDataRepository extends JpaRepository<DraftCaseDataEntity, UUID> {

    Optional<DraftCaseDataEntity> findByCaseReference(long caseReference);

    boolean existsByCaseReference(long caseReference);

    void deleteByCaseReference(long caseReference);

}
