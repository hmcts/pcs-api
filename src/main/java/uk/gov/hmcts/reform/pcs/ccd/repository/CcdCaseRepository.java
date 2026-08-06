package uk.gov.hmcts.reform.pcs.ccd.repository;

import lombok.AllArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@AllArgsConstructor
public class CcdCaseRepository {

    private final JdbcTemplate jdbcTemplate;
    private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    public List<Long> findExpiredDraftCases(int discardAfterDays, int maxBatchLimit) {
        SqlParameterSource namedParameters = new MapSqlParameterSource()
                .addValue("discardDaysAfter", discardAfterDays)
                .addValue("batchLimit", maxBatchLimit);
        return namedParameterJdbcTemplate.query(
                """
                   SELECT cd.reference
                   FROM ccd.case_data cd
                   WHERE cd.created_date < now()::date - :discardDaysAfter
                   AND cd.state in ('AWAITING_SUBMISSION_TO_HMCTS', 'PENDING_CASE_ISSUED')
                """,
                namedParameters,
                (rs, rowNum) -> rs.getLong("reference")
        );
    }

    public List<Long> findExpiredDraftCasesInDraftDiscardedState(int maxBatchLimit) {
        SqlParameterSource namedParameters = new MapSqlParameterSource()
                .addValue("batchLimit", maxBatchLimit);
        return namedParameterJdbcTemplate.query(
                """
                   SELECT cd.reference
                   FROM ccd.case_data cd
                   WHERE cd.state in ('DRAFT_DISCARDED')
                """,
                namedParameters,
                (rs, rowNum) -> rs.getLong("reference")
        );
    }

    public void deleteCcdCaseData(long caseReference) {
        String sql = """
            WITH deleted_case_data AS (
                DELETE FROM ccd.case_data
                WHERE reference = ?
                RETURNING id
            ),
            deleted_case_event AS (
                DELETE FROM ccd.case_event
                WHERE case_data_id IN (SELECT id FROM deleted_case_data)
                RETURNING id
            ),
            deleted_case_event_audit AS (
                DELETE FROM ccd.case_event_audit
                WHERE case_event_id IN (SELECT id FROM deleted_case_event)
                RETURNING case_event_id
            )
            DELETE FROM ccd.case_event_significant_items
            WHERE case_event_id IN (SELECT case_event_id FROM deleted_case_event_audit);
            """;
        jdbcTemplate.update(sql, caseReference);
    }
}
