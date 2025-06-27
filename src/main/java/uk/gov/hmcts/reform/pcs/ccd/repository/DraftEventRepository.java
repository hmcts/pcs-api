package uk.gov.hmcts.reform.pcs.ccd.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import uk.gov.hmcts.reform.pcs.ccd.entity.DraftEventEntity;
import uk.gov.hmcts.reform.pcs.ccd.event.EventId;

import java.util.Optional;
import java.util.UUID;

public interface DraftEventRepository extends JpaRepository<DraftEventEntity, UUID> {

    @Query("SELECT d FROM DraftEventEntity d WHERE d.caseReference = :caseReference "
        + "AND d.userId = :userId AND d.eventId = :eventId")
    Optional<DraftEventEntity> findDraft(@Param("caseReference") long caseReference,
                                         @Param("userId") UUID userId,
                                         @Param("eventId") EventId eventId);


    @Modifying
    @Query("DELETE FROM DraftEventEntity d WHERE d.caseReference = :caseReference "
        + "AND d.userId = :userId AND d.eventId = :eventId")
    void deleteDraft(@Param("caseReference") long caseReference,
                     @Param("userId") UUID userId,
                     @Param("eventId") EventId eventId);

}
