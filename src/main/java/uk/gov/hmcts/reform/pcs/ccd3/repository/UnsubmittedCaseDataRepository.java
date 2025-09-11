package uk.gov.hmcts.reform.pcs.ccd3.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import uk.gov.hmcts.reform.pcs.ccd3.entity.UnsubmittedCaseDataEntity;

import java.util.Optional;
import java.util.UUID;

public interface UnsubmittedCaseDataRepository extends JpaRepository<UnsubmittedCaseDataEntity, UUID> {

    Optional<UnsubmittedCaseDataEntity> findByCaseReference(long caseReference);

    boolean existsByCaseReference(long caseReference);

    void deleteByCaseReference(long caseReference);

}
