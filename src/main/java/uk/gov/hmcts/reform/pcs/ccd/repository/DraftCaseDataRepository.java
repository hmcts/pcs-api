package uk.gov.hmcts.reform.pcs.ccd.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import uk.gov.hmcts.reform.pcs.ccd.entity.DraftCaseDataEntity;
import uk.gov.hmcts.reform.pcs.ccd.event.EventId;

import java.util.Optional;
import java.util.UUID;

public interface DraftCaseDataRepository extends JpaRepository<DraftCaseDataEntity, UUID> {

    Optional<DraftCaseDataEntity> findByCaseReferenceAndEventId(long caseReference, EventId eventId);

    boolean existsByCaseReferenceAndEventId(long caseReference, EventId eventId);

    void deleteByCaseReferenceAndEventId(long caseReference, EventId eventId);

}
