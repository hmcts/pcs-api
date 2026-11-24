package uk.gov.hmcts.reform.pcs.ccd.repository;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import uk.gov.hmcts.reform.pcs.ccd.domain.respondpossessionclaim.ClaimantClaimSummary;

import java.util.List;

@Repository
public class ClaimantCaseSummaryRepository {

    private static final String CASES_CREATED_BY_SQL = """
        SELECT pc.case_reference,
               cd.state,
               a.address_line1,
               a.address_line2,
               a.address_line3,
               a.post_town,
               a.county,
               a.postcode,
               a.id AS address_id,
               (SELECT fp.status
                FROM fee_payment fp
                JOIN claim c ON c.id = fp.possession_claim_id
                WHERE c.case_id = pc.id
                AND fp.payment_callback_handler_type = 'CLAIM'
                AND fp.service_request_reference IS NOT NULL
                ORDER BY fp.id DESC
                LIMIT 1) AS payment_status
        FROM party p
        JOIN pcs_case pc ON pc.id = p.case_id
        JOIN ccd.case_data cd ON cd.reference = pc.case_reference
        LEFT JOIN address a ON a.id = pc.property_address_id
        WHERE p.idam_id = :idamId
        AND p.claim_creator = true
        AND p.removed = false
        ORDER BY pc.case_reference DESC
        """;

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public ClaimantCaseSummaryRepository(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * The cases the user created as claimant, newest first, read in one query from this service's own tables:
     * the case state is held in the decentralised case store, so CCD does not need to be called. The payment status
     * is that of the claim fee's latest payment request, and is null until a fee has been requested.
     */
    public List<ClaimantClaimSummary> findCasesCreatedBy(String idamId) {
        return jdbcTemplate.query(
            CASES_CREATED_BY_SQL,
            new MapSqlParameterSource("idamId", idamId),
            (rs, rowNum) -> ClaimantClaimSummary.builder()
                .caseReference(String.valueOf(rs.getLong("case_reference")))
                .state(rs.getString("state"))
                .paymentStatus(rs.getString("payment_status"))
                .propertyAddress(rs.getObject("address_id") == null ? null
                    : ClaimantClaimSummary.PropertyAddress.builder()
                        .addressLine1(rs.getString("address_line1"))
                        .addressLine2(rs.getString("address_line2"))
                        .addressLine3(rs.getString("address_line3"))
                        .postTown(rs.getString("post_town"))
                        .county(rs.getString("county"))
                        .postcode(rs.getString("postcode"))
                        .build())
                .build());
    }
}
