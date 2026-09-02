package uk.gov.hmcts.reform.pcs.ccd.repository;

import lombok.AllArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.stereotype.Repository;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.model.DeletionCaseData;

import java.util.List;

@Repository
@AllArgsConstructor
public class CcdCaseRepository {

    private final JdbcTemplate jdbcTemplate;
    private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    public List<DeletionCaseData> findExpiredDraftCases(int discardAfterDays, int limit) {
        SqlParameterSource namedParameters = new MapSqlParameterSource()
            .addValue("discardDaysAfter", discardAfterDays)
            .addValue("limit", limit);
        return namedParameterJdbcTemplate.query(
            """
                SELECT cd.reference, cd.state
                FROM ccd.case_data cd
                WHERE cd.last_state_modified_date < now()::date - :discardDaysAfter
                AND cd.state in ('AWAITING_SUBMISSION_TO_HMCTS', 'PENDING_CASE_ISSUED', 'DRAFT_DISCARDED')
                ORDER BY cd.created_date ASC
                LIMIT :limit
            """,
            namedParameters,
                (rs, rowNum) -> DeletionCaseData.builder()
                        .caseRef(rs.getLong("reference"))
                        .state(State.valueOf(rs.getString("state")))
                        .build()
        );
    }

    public void deleteCcdCaseData(long caseReference) {
        String sql = """
            WITH target_case AS (
                SELECT id FROM ccd.case_data WHERE reference = ?
            ),
            deleted_case_event AS (
                DELETE FROM ccd.case_event
                WHERE case_data_id IN (SELECT id FROM target_case)
                RETURNING id
            ),
            deleted_case_event_audit AS (
                DELETE FROM ccd.case_event_audit
                WHERE case_event_id IN (SELECT id FROM deleted_case_event)
            ),
            deleted_significant_items AS (
                DELETE FROM ccd.case_event_significant_items
                WHERE case_event_id IN (SELECT id FROM deleted_case_event)
            )
            DELETE FROM ccd.case_data
            WHERE id IN (SELECT id FROM target_case);
            """;
        jdbcTemplate.update(sql, caseReference);
    }
}
