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
     * Letters with a send after {@code sentAfter} that are due a check: every pending letter, any letter
     * Send Letter has not yet answered for, and any other letter last answered for before
     * {@code recheckBefore}, since Send Letter's operators can still correct a final status (an aborted
     * letter marked posted, say). Least recently attempted first, so each run carries on from where the
     * last one stopped and a letter Send Letter keeps failing on cannot hold up the rest.
     */
    @Query("select letter from LetterEntity letter "
        + "where (letter.status in :pending or letter.lastCheckedAt is null "
        + "or letter.lastCheckedAt < :recheckBefore) "
        + "and exists (select 1 from ClaimActivityLogEntity activity "
        + "where activity.letter = letter and activity.createdAt >= :sentAfter) "
        + "order by letter.lastAttemptedAt asc nulls first")
    List<LetterEntity> findToSync(@Param("pending") Collection<LetterStatus> pending,
                                  @Param("recheckBefore") LocalDateTime recheckBefore,
                                  @Param("sentAfter") LocalDateTime sentAfter,
                                  Pageable pageable);

    /**
     * Records a letter Send Letter accepted, unless it is already here: Send Letter answers an identical
     * request with an earlier letter's id, and the rollout catch-up may have just created it.
     */
    @Modifying
    @Query(nativeQuery = true, value = "INSERT INTO letter (letter_id, status) VALUES (:letterId, 'CREATED') "
        + "ON CONFLICT (letter_id) DO NOTHING")
    void createIfAbsent(@Param("letterId") UUID letterId);

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

    /**
     * Links unlinked {@code PACK_SENT} rows to their letter. Only rows whose letter exists: a row committed
     * after {@link #createLettersForUnlinkedPackSentRows} ran waits for the next run.
     */
    @Modifying
    @Transactional
    @Query(nativeQuery = true, value = "UPDATE claim_activity_log activity "
        + "SET letter_id = letter.letter_id FROM letter "
        + "WHERE letter.letter_id = CAST(activity.details ->> 'letterId' AS uuid) "
        + "AND activity.activity_type = 'PACK_SENT' AND activity.status = 'SUCCESS' "
        + "AND activity.letter_id IS NULL AND activity.created_at >= :sentAfter")
    int linkUnlinkedPackSentRows(@Param("sentAfter") LocalDateTime sentAfter);

    @Query("select distinct activity.pcsCase.caseReference from ClaimActivityLogEntity activity "
        + "where activity.letter.letterId = :letterId")
    List<Long> findCaseReferences(@Param("letterId") UUID letterId);
}
