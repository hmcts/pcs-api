package uk.gov.hmcts.reform.pcs.ccd.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.reform.pcs.ccd.entity.LetterEntity;
import uk.gov.hmcts.reform.pcs.ccd.service.bulkprint.LetterStatus;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Repository
public interface LetterRepository extends JpaRepository<LetterEntity, UUID> {

    /**
     * Letters with a send after {@code sentAfter} that are due a check: every pending letter, and any other
     * letter last checked before {@code recheckBefore}, since Send Letter's operators can still correct a
     * final status (an aborted letter marked posted, say). Least recently checked first, so each run carries
     * on from where the last one stopped.
     */
    @Query("select letter from LetterEntity letter "
        + "where (letter.status in :pending or letter.lastCheckedAt < :recheckBefore) "
        + "and exists (select 1 from ClaimActivityLogEntity activity "
        + "where activity.letter = letter and activity.createdAt >= :sentAfter) "
        + "order by letter.lastCheckedAt asc nulls first")
    List<LetterEntity> findToSync(@Param("pending") Collection<LetterStatus> pending,
                                  @Param("recheckBefore") LocalDateTime recheckBefore,
                                  @Param("sentAfter") LocalDateTime sentAfter,
                                  Pageable pageable);

    /**
     * Creates a letter for any {@code PACK_SENT} row that names one but is not linked to it (a row an older
     * pod wrote during a rollout). Pair with {@link #linkUnlinkedPackSentRows}.
     */
    @Modifying
    @Transactional
    @Query(nativeQuery = true, value = "INSERT INTO letter (letter_id, status) "
        + "SELECT DISTINCT CAST(details ->> 'letterId' AS uuid), 'CREATED' FROM claim_activity_log "
        + "WHERE activity_type = 'PACK_SENT' AND status = 'SUCCESS' AND letter_id IS NULL "
        + "AND details ->> 'letterId' IS NOT NULL AND created_at >= :sentAfter "
        + "ON CONFLICT (letter_id) DO NOTHING")
    int createLettersForUnlinkedPackSentRows(@Param("sentAfter") LocalDateTime sentAfter);

    @Modifying
    @Transactional
    @Query(nativeQuery = true, value = "UPDATE claim_activity_log SET letter_id = CAST(details ->> 'letterId' AS uuid) "
        + "WHERE activity_type = 'PACK_SENT' AND status = 'SUCCESS' AND letter_id IS NULL "
        + "AND details ->> 'letterId' IS NOT NULL AND created_at >= :sentAfter")
    int linkUnlinkedPackSentRows(@Param("sentAfter") LocalDateTime sentAfter);

    @Query("select distinct activity.pcsCase.caseReference from ClaimActivityLogEntity activity "
        + "where activity.letter.letterId = :letterId")
    List<Long> findCaseReferences(@Param("letterId") UUID letterId);
}
