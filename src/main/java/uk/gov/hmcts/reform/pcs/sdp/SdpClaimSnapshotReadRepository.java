package uk.gov.hmcts.reform.pcs.sdp;

import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Repository
public class SdpClaimSnapshotReadRepository {

    private static final String HEADER_SQL = """
        SELECT c.id AS claim_id,
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
               n.notice_date
        FROM claim c
        JOIN pcs_case pc ON pc.id = c.case_id
        LEFT JOIN tenancy_licence tl ON tl.case_id = pc.id
        LEFT JOIN notice_of_possession n ON n.claim_id = c.id
        WHERE c.id IN (:ids)
        """;

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

    private static final String PAYMENT_SQL = """
        SELECT fp.possession_claim_id AS claim_id,
               fp.external_reference,
               fp.status,
               fp.request_date
        FROM fee_payment fp
        WHERE fp.possession_claim_id IN (:ids)
        """;

    private static final String GROUND_SQL = """
        SELECT g.claim_id,
               g.code,
               g.description,
               g.category
        FROM claim_ground g
        WHERE g.claim_id IN (:ids)
        """;

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

    public SdpClaimSnapshotReadRepository(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
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

    private <T> List<T> query(String sql, Collection<UUID> ids, RowMapper<T> mapper) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        return jdbcTemplate.query(sql, new MapSqlParameterSource("ids", ids), mapper);
    }

    private SdpClaimSnapshotHeader mapHeader(ResultSet rs, int rowNum) throws SQLException {
        return new SdpClaimSnapshotHeader(
            rs.getObject("claim_id", UUID.class),
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
            rs.getObject("notice_date", LocalDate.class)
        );
    }

    private SdpClaimSnapshotParty mapParty(ResultSet rs, int rowNum) throws SQLException {
        return new SdpClaimSnapshotParty(
            rs.getObject("claim_id", UUID.class),
            rs.getObject("party_id", UUID.class),
            rs.getString("role"),
            rs.getBoolean("represented")
        );
    }

    private SdpClaimSnapshotPayment mapPayment(ResultSet rs, int rowNum) throws SQLException {
        return new SdpClaimSnapshotPayment(
            rs.getObject("claim_id", UUID.class),
            rs.getString("external_reference"),
            rs.getString("status"),
            toLocalDateTime(rs, "request_date")
        );
    }

    private SdpClaimSnapshotGround mapGround(ResultSet rs, int rowNum) throws SQLException {
        return new SdpClaimSnapshotGround(
            rs.getObject("claim_id", UUID.class),
            rs.getString("code"),
            rs.getString("description"),
            rs.getString("category")
        );
    }

    private SdpClaimSnapshotResponse mapResponse(ResultSet rs, int rowNum) throws SQLException {
        return new SdpClaimSnapshotResponse(
            rs.getObject("claim_id", UUID.class),
            rs.getObject("response_id", Integer.class),
            rs.getObject("party_id", UUID.class),
            rs.getString("make_counter_claim"),
            toLocalDateTime(rs, "response_submitted_date")
        );
    }

    private static LocalDateTime toLocalDateTime(ResultSet rs, String column) throws SQLException {
        Timestamp timestamp = rs.getTimestamp(column);
        return timestamp == null ? null : timestamp.toLocalDateTime();
    }
}
