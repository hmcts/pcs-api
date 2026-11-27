package uk.gov.hmcts.reform.pcs.ccd.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import uk.gov.hmcts.reform.pcs.ccd.entity.LetterEntity;
import uk.gov.hmcts.reform.pcs.ccd.service.bulkprint.LetterStatus;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Repository
public interface LetterRepository extends JpaRepository<LetterEntity, UUID> {

    /**
     * Letters still in one of {@code statuses} with a send after {@code sentAfter}, least recently checked
     * first, so each run carries on from where the last one stopped.
     */
    @Query("select letter from LetterEntity letter "
        + "where letter.status in :statuses "
        + "and exists (select 1 from ClaimActivityLogEntity activity "
        + "where activity.letter = letter and activity.createdAt >= :sentAfter) "
        + "order by letter.lastCheckedAt asc nulls first")
    List<LetterEntity> findToSync(@Param("statuses") Collection<LetterStatus> statuses,
                                  @Param("sentAfter") LocalDateTime sentAfter,
                                  Pageable pageable);

    /**
     * Records a letter Send Letter accepted, unless it is already here: Send Letter answers an identical
     * request with an earlier letter's id.
     */
    @Modifying
    @Query(nativeQuery = true, value = "INSERT INTO letter (letter_id, status) VALUES (:letterId, 'CREATED') "
        + "ON CONFLICT (letter_id) DO NOTHING")
    void createIfAbsent(@Param("letterId") UUID letterId);

    @Query("select distinct activity.pcsCase.caseReference from ClaimActivityLogEntity activity "
        + "where activity.letter.letterId = :letterId")
    List<Long> findCaseReferences(@Param("letterId") UUID letterId);
}
