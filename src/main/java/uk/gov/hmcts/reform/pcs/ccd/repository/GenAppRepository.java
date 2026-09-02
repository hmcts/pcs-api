package uk.gov.hmcts.reform.pcs.ccd.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import uk.gov.hmcts.reform.pcs.ccd.entity.GenAppEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface GenAppRepository extends JpaRepository<GenAppEntity, UUID> {

    boolean existsByPcsCaseAndClientReference(PcsCaseEntity pcsCaseEntity, String clientReference);

    /**
     * Card payments deliver the same service request callback twice, so concurrent callers must be
     * serialised to prevent both observing the pre-issue state and issuing the application twice.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select genApp from GenAppEntity genApp where genApp.id = :id")
    Optional<GenAppEntity> findByIdForUpdate(@Param("id") UUID id);

}
