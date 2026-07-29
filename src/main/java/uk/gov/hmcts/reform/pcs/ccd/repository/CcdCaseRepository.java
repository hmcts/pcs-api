package uk.gov.hmcts.reform.pcs.ccd.repository;

import lombok.AllArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@AllArgsConstructor
public class CcdCaseRepository {

    private final JdbcTemplate jdbcTemplate;

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
