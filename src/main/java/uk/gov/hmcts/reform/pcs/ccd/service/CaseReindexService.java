package uk.gov.hmcts.reform.pcs.ccd.service;

import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;

/**
 * Queues a single case for Elasticsearch reindexing.
 *
 * <p>CaseAccessGroups is derived at read time, so it only reaches the search index when the
 * indexer refetches the case — which normally happens because every CCD event writes
 * {@code ccd.case_data} and a trigger enqueues the case. Writes that change derivation inputs
 * outside an event (e.g. a Notice of Change updating {@code claim_party_organisation}) must
 * enqueue the case themselves, or the case list serves stale visibility: the incoming
 * organisation never sees the case listed and the outgoing organisation keeps a ghost row.
 *
 * <p>The insert mirrors the SDK's own {@code enqueue_case_revision} trigger (conflict clause
 * included) but without touching the case row. Replace with the SDK's
 * {@code CaseReindexingService.reindex(caseRef)} once that API exists upstream.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class CaseReindexService {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public void reindex(long caseReference) {
        int queued = jdbcTemplate.update("""
            insert into ccd.es_queue(reference, case_revision, enqueued_at)
            select reference, case_revision, now()
            from ccd.case_data
            where reference = :reference
            on conflict (reference) do update
              set case_revision = greatest(ccd.es_queue.case_revision, excluded.case_revision),
                  enqueued_at   = least(ccd.es_queue.enqueued_at, excluded.enqueued_at)
            """,
            Map.of("reference", caseReference));
        log.info("Queued case {} for search reindexing ({} row)", caseReference, queued);
    }
}
