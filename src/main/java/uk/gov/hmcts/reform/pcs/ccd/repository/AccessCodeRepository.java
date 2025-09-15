package uk.gov.hmcts.reform.pcs.ccd.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import uk.gov.hmcts.reform.pcs.ccd.entity.AccessCodeEntity;

import java.util.Optional;
import java.util.UUID;

public interface AccessCodeRepository extends JpaRepository<AccessCodeEntity, UUID> {

    Optional<AccessCodeEntity> findByCaseReferenceAndCode(long caseReference, String code);

}
