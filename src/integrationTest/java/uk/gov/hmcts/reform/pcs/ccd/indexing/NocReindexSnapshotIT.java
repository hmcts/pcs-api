package uk.gov.hmcts.reform.pcs.ccd.indexing;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.containers.PostgreSQLContainer;

import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Proves the reindex-only approach to HDPI-8685 wrong: re-queueing the case's
 * current revision, but {@code DecentralisedESIndexer} builds the document from the stored
 * {@code ccd.case_event.data} snapshot of the last event, never from a fresh service read. A Notice of
 * Change writes no event, so the re-queued document still carries the pre-NoC CaseAccessGroups.
 *
 * <p>Runs the SDK's own {@code dataruntime-db} migrations (real tables, real enqueue trigger), the real
 * the proposed reindex queue insert, and the indexer's {@code claimBatch} SQL verbatim.
 * No Spring context, no Elasticsearch: the SQL row IS the document the indexer would bulk-index.
 */
@DisplayName("NoC reindex replays the stored event snapshot")
class NocReindexSnapshotIT {

    private static final long CASE_REFERENCE = 1787849985141941L;
    private static final long CASE_DATA_ID = 17246000L;
    private static final String CLAIMANT_GROUP = "PCS:PCS:prof-org-claimant-access:claimant:HC9X8YK";
    private static final String INCOMING_SOLICITOR_GROUP =
        "PCS:PCS:solicitor-org-defendant-access:defendant-solicitor:XXUW0T9";

    /** Copied verbatim from DecentralisedESIndexer.claimBatch() (sdk/ccd-runtime-indexing). */
    private static final String INDEXER_CLAIM_BATCH_SQL = """
          with next_batch as (
              select reference, case_revision
              from ccd.es_queue
              where locked_until is null
                 or locked_until < now()
              order by enqueued_at
              limit ?
              for update skip locked
          ),
          claimed as (
              update ccd.es_queue q
              set locked_until = now() + (? * interval '1 second'),
                  lock_token = ?
              from next_batch nb
              where q.reference = nb.reference
                and q.case_revision = nb.case_revision
              returning q.reference, q.case_revision
          )
          select
              row_to_json(row)::jsonb as row,
              row.reference,
              row.case_revision,
              row.id as case_data_id,
              row.event_id,
              row.index_id
          from (
              select
                  cd.reference,
                  c.case_revision,
                  cd.case_type_id,
                  lower(cd.case_type_id) || '_cases' as index_id,
                  cd.created_date,
                  cd.jurisdiction,
                  cd.id as id,
                  cd.state,
                  cd.security_classification,
                  cd.last_state_modified_date,
                  cd.supplementary_data,
                  ce.id as event_id,
                  ce.data,
                  coalesce(cd.last_modified, cd.created_date) as last_modified
              from claimed c
              join ccd.case_data cd on cd.reference = c.reference
              join lateral (
                  select ce.*
                  from ccd.case_event ce
                  where ce.case_data_id = cd.id
                    and ce.case_revision <= c.case_revision
                  order by ce.case_revision desc, ce.id desc
                  limit 1
              ) ce on true
          ) row
          """;

    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");
    private static NamedParameterJdbcTemplate jdbc;
    private final ObjectMapper mapper = new ObjectMapper();

    @BeforeAll
    static void migrateSdkSchema() {
        POSTGRES.start();
        DriverManagerDataSource dataSource = new DriverManagerDataSource(
            POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());

        // Same configuration as the SDK's DecentralisedFlywayAutoConfiguration.
        Properties flywayProperties = new Properties();
        flywayProperties.setProperty("flyway.postgresql.transactional.lock", "false");
        Flyway.configure()
            .dataSource(dataSource)
            .locations("classpath:dataruntime-db/migration")
            .schemas("ccd")
            .configuration(flywayProperties)
            .load()
            .migrate();

        jdbc = new NamedParameterJdbcTemplate(dataSource);
    }

    @BeforeEach
    void caseIssuedThenNocAppliedWithoutAnEvent() {
        jdbc.getJdbcOperations().execute("delete from ccd.es_queue");
        jdbc.getJdbcOperations().execute("delete from ccd.case_event");
        jdbc.getJdbcOperations().execute("delete from ccd.case_data");

        // The case as the runtime leaves it after create -> resume -> claimIssuePayment (revision 3).
        jdbc.update("""
            insert into ccd.case_data (reference, id, version, created_date, last_modified, security_classification,
                                       jurisdiction, case_type_id, state, data, supplementary_data, case_revision)
            values (:ref, :id, 1, now(), now(), 'PUBLIC', 'PCS', 'PCS', 'CASE_ISSUED', '{}', '{}', 3)
            """, Map.of("ref", CASE_REFERENCE, "id", CASE_DATA_ID));
        storeEventSnapshot(1, "createPossessionClaim", List.of(CLAIMANT_GROUP));
        storeEventSnapshot(2, "resumePossessionClaim", List.of(CLAIMANT_GROUP));
        storeEventSnapshot(3, "claimIssuePayment", List.of(CLAIMANT_GROUP));

        // Indexer has drained everything the events queued; ES now holds the revision-3 snapshot.
        jdbc.getJdbcOperations().execute("delete from ccd.es_queue");

        // Notice of Change applied: LegalRepresentativePartyLinkService writes claim_party_organisation in the
        // pcs schema only. Nothing here touches ccd.case_data or ccd.case_event - which is the whole point.
    }

    @Test
    @DisplayName("reindexCase() re-queues revision 3 and the indexer would re-emit the pre-NoC groups")
    void reindexAfterNocReplaysStaleSnapshot() throws Exception {
        assertThat(queueDepth()).as("NoC itself queues nothing").isZero();

        int queued = reindexCase(CASE_REFERENCE);

        assertThat(queued).isEqualTo(1);
        assertThat(queuedRevision()).as("queued at the case's current revision").isEqualTo(3);

        JsonNode document = claimAsIndexerWould();
        List<String> groups = caseAccessGroupIds(document);

        System.out.println("INDEXER DOCUMENT AFTER reindexCase(): event=" + eventName(document)
                               + " revision=" + document.get("case_revision") + " groups=" + groups);

        assertThat(eventName(document)).isEqualTo("claimIssuePayment");
        assertThat(groups)
            .as("document the indexer would write after NoC + reindexCase()")
            .containsExactly(CLAIMANT_GROUP)
            .doesNotContain(INCOMING_SOLICITOR_GROUP);
    }

    @Test
    @DisplayName("control: a real case event stores a fresh snapshot and the same reindex carries the new org")
    void reindexAfterAnEventCarriesNewRepresentation() throws Exception {
        // What a system 'noticeOfChangeApplied' event would do: fresh view snapshot at revision 4.
        storeEventSnapshot(4, "noticeOfChangeApplied", List.of(CLAIMANT_GROUP, INCOMING_SOLICITOR_GROUP));
        jdbc.update("update ccd.case_data set case_revision = 4, last_modified = now() where reference = :ref",
                    Map.of("ref", CASE_REFERENCE));
        jdbc.getJdbcOperations().execute("delete from ccd.es_queue");

        reindexCase(CASE_REFERENCE);
        JsonNode document = claimAsIndexerWould();
        List<String> groups = caseAccessGroupIds(document);

        System.out.println("INDEXER DOCUMENT AFTER EVENT + reindexCase(): event=" + eventName(document)
                               + " revision=" + document.get("case_revision") + " groups=" + groups);

        assertThat(eventName(document)).isEqualTo("noticeOfChangeApplied");
        assertThat(groups).containsExactly(CLAIMANT_GROUP, INCOMING_SOLICITOR_GROUP);
    }

    /** The queue insert proposed in dtsse-ccd-config-generator #1077 (CaseReindexingService.reindexCase), verbatim. */
    private static int reindexCase(long caseReference) {
        return jdbc.update("""
            insert into ccd.es_queue(reference, case_revision, enqueued_at)
            select reference, case_revision, now()
            from ccd.case_data
            where reference = :reference
            on conflict (reference) do update
            set case_revision = greatest(ccd.es_queue.case_revision, excluded.case_revision),
                enqueued_at = least(ccd.es_queue.enqueued_at, excluded.enqueued_at)
            """, Map.of("reference", caseReference));
    }

    // Mirrors AuditEventService.saveAuditRecord(): data = the service view serialised at event time.
    private void storeEventSnapshot(long revision, String eventId, List<String> caseAccessGroupIds) {
        String groupsJson = caseAccessGroupIds.stream()
            .map(id -> "{\"id\":\"" + UUID.nameUUIDFromBytes(id.getBytes()) + "\",\"value\":{"
                + "\"caseAccessGroupId\":\"" + id + "\",\"caseAccessGroupType\":\"CCD:all-cases-access\"}}")
            .reduce((a, b) -> a + "," + b).orElse("");
        String data = "{\"CaseAccessGroups\":[" + groupsJson + "]}";

        jdbc.update("""
            insert into ccd.case_event (created_date, security_classification, case_data_id, case_type_version,
                                        event_id, user_id, case_type_id, state_id, data, user_first_name,
                                        user_last_name, event_name, state_name, version, case_revision,
                                        idempotency_key)
            values (now(), 'PUBLIC', :caseDataId, 1, :eventId, 'user', 'PCS', 'CASE_ISSUED', cast(:data as jsonb),
                    'Test', 'User', :eventId, 'Case issued', 1, :revision, :key)
            """, Map.of("caseDataId", CASE_DATA_ID, "eventId", eventId, "data", data,
                        "revision", revision, "key", UUID.randomUUID()));
    }

    private JsonNode claimAsIndexerWould() throws Exception {
        List<Map<String, Object>> rows = jdbc.getJdbcOperations()
            .queryForList(INDEXER_CLAIM_BATCH_SQL, 10, 60, UUID.randomUUID());
        assertThat(rows).as("one claimed queue entry").hasSize(1);
        return mapper.readTree(rows.getFirst().get("row").toString());
    }

    private List<String> caseAccessGroupIds(JsonNode indexerRow) {
        JsonNode groups = indexerRow.path("data").path("CaseAccessGroups");
        return groups.findValues("caseAccessGroupId").stream().map(JsonNode::asText).toList();
    }

    /** The indexer row's event_id is ccd.case_event.id; resolve it to the event name for readable assertions. */
    private String eventName(JsonNode indexerRow) {
        return jdbc.queryForObject("select event_id from ccd.case_event where id = :id",
                                   Map.of("id", indexerRow.get("event_id").asLong()), String.class);
    }

    private int queueDepth() {
        return jdbc.getJdbcOperations().queryForObject("select count(*) from ccd.es_queue", Integer.class);
    }

    private long queuedRevision() {
        return jdbc.queryForObject("select case_revision from ccd.es_queue where reference = :ref",
                                   Map.of("ref", CASE_REFERENCE), Long.class);
    }
}
