package uk.gov.hmcts.reform.pcs.ccd.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import uk.gov.hmcts.reform.pcs.ccd.entity.DraftCaseDataEntity;
import uk.gov.hmcts.reform.pcs.ccd.event.EventId;

import java.util.Optional;
import java.util.UUID;

public interface DraftCaseDataRepository extends JpaRepository<DraftCaseDataEntity, UUID> {

    Optional<DraftCaseDataEntity> findByCaseReferenceAndEventIdAndIdamUserId(
        long caseReference, EventId eventId, UUID idamUserId);

    boolean existsByCaseReferenceAndEventIdAndIdamUserId(
        long caseReference, EventId eventId, UUID idamUserId);

    @Modifying
    @Query("DELETE FROM DraftCaseDataEntity d "
        + "WHERE d.caseReference = :caseReference AND d.eventId = :eventId AND d.idamUserId = :idamUserId")
    void deleteByCaseReferenceAndEventIdAndIdamUserId(@Param("caseReference") long caseReference,
                                                      @Param("eventId") EventId eventId,
                                                      @Param("idamUserId") UUID idamUserId);

}
