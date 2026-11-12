package uk.gov.hmcts.reform.pcs.sdp;

import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.stereotype.Repository;
import uk.gov.hmcts.reform.pcs.ccd.entity.SdpClaimSnapshotEntity;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Repository
public class SdpClaimSnapshotJdbcRepository {

    private static final String CLAIM_ID = "claim_id";
    private static final String PARTY_ID = "party_id";

    // Claim, case, tenancy, notice and property address fields for the snapshot,
    // plus the current version if already exported.
    private static final String HEADER_SQL = """
        SELECT DISTINCT c.id AS claim_id,
               c.claimant_type,
               c.against_trespassers,
               c.language_used,
               c.claim_submitted_date,
               c.claim_issued_date,
               pc.case_reference,
               pc.case_management_location,
               pc.legislative_country,
               tl.type AS tenancy_type,
               n.serving_method,
               n.notice_date,
               s.snapshot_version,
               a.address_line1,
               a.address_line2,
               a.address_line3,
               a.post_town,
               a.county,
               a.country,
               a.postcode
        FROM claim c
        JOIN pcs_case pc ON pc.id = c.case_id
        LEFT JOIN tenancy_licence tl ON tl.case_id = pc.id
        LEFT JOIN notice_of_possession n ON n.claim_id = c.id
        LEFT JOIN sdp_claim_snapshot s ON s.claim_id = c.id
        LEFT JOIN address a ON a.id = pc.property_address_id
        WHERE c.id IN (:ids)
        """;

    // Claims with no snapshot yet, or with source data changed since last export.
    private static final String ID_SQL = """
        SELECT DISTINCT c.id AS claim_id
        FROM claim c
        LEFT JOIN sdp_claim_snapshot s ON s.claim_id = c.id
        LEFT JOIN pcs_case pc ON pc.id = c.case_id
        LEFT JOIN tenancy_licence tl ON tl.case_id = pc.id
        LEFT JOIN notice_of_possession n ON n.claim_id = c.id
        WHERE s.claim_id IS NULL
           OR c.last_modified > s.exported_at
           OR pc.last_modified_datetime > s.exported_at
           OR tl.last_modified > s.exported_at
           OR n.last_modified > s.exported_at
           OR EXISTS (
                SELECT 1 FROM fee_payment fp
                WHERE fp.possession_claim_id = c.id AND fp.last_modified > s.exported_at
           )
           OR EXISTS (
                SELECT 1 FROM claim_party cp
                WHERE cp.claim_id = c.id AND cp.last_modified > s.exported_at
           )
           OR EXISTS (
                SELECT 1 FROM claim_party_organisation o
                JOIN claim_party cp ON cp.party_id = o.party_id
                WHERE cp.claim_id = c.id AND o.last_modified > s.exported_at
           )
           OR EXISTS (
                SELECT 1 FROM claim_ground g
                WHERE g.claim_id = c.id AND g.last_modified > s.exported_at
           )
           OR EXISTS (
                SELECT 1 FROM defendant_response r
                WHERE r.claim_id = c.id AND r.last_modified > s.exported_at
           )
        ORDER BY c.id
        """;

    // Insert a snapshot bookmark or update version and exported_at when the claim already has a row.
    private static final String UPSERT_SQL = """
        INSERT INTO sdp_claim_snapshot (claim_id, snapshot_version, exported_at)
        VALUES (:claimId, :snapshotVersion, :exportedAt)
        ON CONFLICT (claim_id) DO UPDATE
        SET snapshot_version = EXCLUDED.snapshot_version,
            exported_at = EXCLUDED.exported_at
        """;

    // Parties on the claim and whether they have an active legal representative.
    private static final String PARTY_SQL = """
        SELECT cp.claim_id,
               cp.party_id,
               cp.role,
               EXISTS (
                   SELECT 1
                   FROM claim_party_organisation o
                   WHERE o.party_id = cp.party_id AND o.active = 'YES'
               ) AS represented
        FROM claim_party cp
        WHERE cp.claim_id IN (:ids)
        """;

    // Fee payments linked to the claim.
    private static final String PAYMENT_SQL = """
        SELECT fp.possession_claim_id AS claim_id,
               fp.id,
               fp.party_id,
               fp.service_request_reference,
               fp.external_reference,
               fp.amount,
               fp.status,
               fp.payment_callback_handler_type,
               fp.request_date
        FROM fee_payment fp
        WHERE fp.possession_claim_id IN (:ids)
        """;

    // Possession grounds on the claim.
    private static final String GROUND_SQL = """
        SELECT g.claim_id,
               g.code,
               g.description,
               g.category
        FROM claim_ground g
        WHERE g.claim_id IN (:ids)
        """;

    // Defendant responses on the claim.
    private static final String RESPONSE_SQL = """
        SELECT r.claim_id,
               r.id AS response_id,
               r.party_id,
               r.make_counter_claim,
               r.response_submitted_date
        FROM defendant_response r
        WHERE r.claim_id IN (:ids)
        """;

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public SdpClaimSnapshotJdbcRepository(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<UUID> findIdsForExport() {
        return jdbcTemplate.query(
            ID_SQL,
            new MapSqlParameterSource(),
            (rs, rowNum) -> rs.getObject(CLAIM_ID, UUID.class)
        );
    }

    public List<SdpClaimSnapshotHeader> findHeadersByClaimId(Collection<UUID> ids) {
        return query(HEADER_SQL, ids, this::mapHeader);
    }

    public List<SdpClaimSnapshotParty> findPartiesByClaimId(Collection<UUID> ids) {
        return query(PARTY_SQL, ids, this::mapParty);
    }

    public List<SdpClaimSnapshotPayment> findPaymentsByClaimId(Collection<UUID> ids) {
        return query(PAYMENT_SQL, ids, this::mapPayment);
    }

    public List<SdpClaimSnapshotGround> findGroundsByClaimId(Collection<UUID> ids) {
        return query(GROUND_SQL, ids, this::mapGround);
    }

    public List<SdpClaimSnapshotResponse> findResponsesByClaimId(Collection<UUID> ids) {
        return query(RESPONSE_SQL, ids, this::mapResponse);
    }

    public void upsertSnapshots(List<SdpClaimSnapshotEntity> snapshots) {
        if (snapshots == null || snapshots.isEmpty()) {
            return;
        }
        SqlParameterSource[] params = snapshots.stream()
            .map(snapshot -> new MapSqlParameterSource()
                .addValue("claimId", snapshot.getClaimId())
                .addValue("snapshotVersion", snapshot.getSnapshotVersion())
                .addValue("exportedAt", snapshot.getExportedAt()))
            .toArray(SqlParameterSource[]::new);
        jdbcTemplate.batchUpdate(UPSERT_SQL, params);
    }

    private <T> List<T> query(String sql, Collection<UUID> ids, RowMapper<T> mapper) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        return jdbcTemplate.query(sql, new MapSqlParameterSource("ids", ids), mapper);
    }

    private SdpClaimSnapshotHeader mapHeader(ResultSet rs, int rowNum) throws SQLException {
        return new SdpClaimSnapshotHeader(
            rs.getObject(CLAIM_ID, UUID.class),
            rs.getString("claimant_type"),
            rs.getString("against_trespassers"),
            rs.getString("language_used"),
            toLocalDateTime(rs, "claim_submitted_date"),
            toLocalDateTime(rs, "claim_issued_date"),
            rs.getObject("case_reference", Long.class),
            rs.getObject("case_management_location", Integer.class),
            rs.getString("legislative_country"),
            rs.getString("tenancy_type"),
            rs.getString("serving_method"),
            rs.getObject("notice_date", LocalDate.class),
            rs.getObject("snapshot_version", Integer.class),
            rs.getString("address_line1"),
            rs.getString("address_line2"),
            rs.getString("address_line3"),
            rs.getString("post_town"),
            rs.getString("county"),
            rs.getString("country"),
            rs.getString("postcode")
        );
    }

    private SdpClaimSnapshotParty mapParty(ResultSet rs, int rowNum) throws SQLException {
        return new SdpClaimSnapshotParty(
            rs.getObject(CLAIM_ID, UUID.class),
            rs.getObject(PARTY_ID, UUID.class),
            rs.getString("role"),
            rs.getBoolean("represented")
        );
    }

    private SdpClaimSnapshotPayment mapPayment(ResultSet rs, int rowNum) throws SQLException {
        return new SdpClaimSnapshotPayment(
            rs.getObject(CLAIM_ID, UUID.class),
            rs.getObject("id", Integer.class),
            rs.getObject(PARTY_ID, UUID.class),
            rs.getString("service_request_reference"),
            rs.getString("external_reference"),
            rs.getBigDecimal("amount"),
            rs.getString("status"),
            rs.getString("payment_callback_handler_type"),
            toLocalDateTime(rs, "request_date")
        );
    }

    private SdpClaimSnapshotGround mapGround(ResultSet rs, int rowNum) throws SQLException {
        return new SdpClaimSnapshotGround(
            rs.getObject(CLAIM_ID, UUID.class),
            rs.getString("code"),
            rs.getString("description"),
            rs.getString("category")
        );
    }

    private SdpClaimSnapshotResponse mapResponse(ResultSet rs, int rowNum) throws SQLException {
        return new SdpClaimSnapshotResponse(
            rs.getObject(CLAIM_ID, UUID.class),
            rs.getObject("response_id", Integer.class),
            rs.getObject(PARTY_ID, UUID.class),
            rs.getString("make_counter_claim"),
            toLocalDateTime(rs, "response_submitted_date")
        );
    }

    private static LocalDateTime toLocalDateTime(ResultSet rs, String column) throws SQLException {
        Timestamp timestamp = rs.getTimestamp(column);
        return timestamp == null ? null : timestamp.toLocalDateTime();
    }
}
