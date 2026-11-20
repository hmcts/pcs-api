package uk.gov.hmcts.reform.pcs.ccd.repository;

import lombok.AllArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.UUID;

/**
 * Hard-deletes PUBLIC-schema document rows that block {@code pcs_case} removal.
 *
 * <p>{@link uk.gov.hmcts.reform.pcs.ccd.entity.DocumentEntity} uses Hibernate {@code @SoftDelete},
 * so JPA cascade REMOVE only soft-deletes documents and leaves {@code party_id} / {@code case_id}
 * foreign keys in place. Native delete is required to remove those rows before the case is deleted.
 */
@Repository
@AllArgsConstructor
public class PcsCaseHardDeleteRepository {

    private final JdbcTemplate jdbcTemplate;

    public void deleteBlockingDependencies(UUID caseId) {
        jdbcTemplate.update("""
            UPDATE claim
            SET claim_form_document_id = NULL
            WHERE claim_form_document_id IN (SELECT id FROM document WHERE case_id = ?)
            """, caseId);
        jdbcTemplate.update("""
            UPDATE general_application
            SET submission_document_id = NULL
            WHERE submission_document_id IN (SELECT id FROM document WHERE case_id = ?)
            """, caseId);
        jdbcTemplate.update("""
            UPDATE orders
            SET document_id = NULL
            WHERE document_id IN (SELECT id FROM document WHERE case_id = ?)
            """, caseId);
        jdbcTemplate.update("""
            UPDATE party_attribute_assertion
            SET evidence_document_id = NULL
            WHERE evidence_document_id IN (SELECT id FROM document WHERE case_id = ?)
            """, caseId);
        jdbcTemplate.update("""
            DELETE FROM claim_document
            WHERE document_id IN (SELECT id FROM document WHERE case_id = ?)
            """, caseId);
        // Native delete bypasses @SoftDelete so party/case FKs are actually removed.
        jdbcTemplate.update("DELETE FROM document WHERE case_id = ?", caseId);
    }
}
