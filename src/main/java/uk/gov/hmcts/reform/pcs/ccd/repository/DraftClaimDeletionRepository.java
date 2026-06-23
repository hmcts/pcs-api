package uk.gov.hmcts.reform.pcs.ccd.repository;

import lombok.AllArgsConstructor;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
@AllArgsConstructor
public class DraftClaimDeletionRepository {

    private static final String DEFENDANT_RESPONSE_IDS_BY_CASE_ID =
        "(SELECT id FROM defendant_response WHERE pcs_case_id = ?)";
    private static final String CLAIM_IDS_BY_CASE_ID = "(SELECT id FROM claim WHERE case_id = ?)";
    private static final String PARTY_IDS_BY_CASE_ID = "(SELECT id FROM party WHERE case_id = ?)";

    private final JdbcTemplate jdbcTemplate;

    public UUID getCaseId(long caseReference) {
        try {
            return jdbcTemplate.queryForObject(
                "SELECT id FROM pcs_case WHERE case_reference = ?",
                UUID.class,
                caseReference
            );
        } catch (EmptyResultDataAccessException e) {
            return null;
        }
    }

    public boolean hasIssuedClaim(UUID caseId) {
        Boolean result = jdbcTemplate.queryForObject(
            "SELECT EXISTS (SELECT 1 FROM claim WHERE case_id = ? AND claim_issued_date IS NOT NULL)",
            Boolean.class,
            caseId
        );
        return Boolean.TRUE.equals(result);
    }

    public void lockClaimsForCase(UUID caseId) {
        jdbcTemplate.queryForList("SELECT id FROM claim WHERE case_id = ? FOR UPDATE", UUID.class, caseId);
    }

    public void deleteDraftCaseData(long caseReference) {
        jdbcTemplate.update("DELETE FROM draft.draft_case_data WHERE case_reference = ?", caseReference);
    }

    public void deleteCcdCaseData(long caseReference) {
        jdbcTemplate.update("DELETE FROM ccd.case_data WHERE reference = ?", caseReference);
    }

    public List<UUID> getAddressIdsForCase(UUID caseId) {
        return jdbcTemplate.queryForList("""
            SELECT property_address_id FROM pcs_case WHERE id = ? AND property_address_id IS NOT NULL
            UNION
            SELECT address_id FROM party WHERE case_id = ? AND address_id IS NOT NULL
            """, UUID.class, caseId, caseId);
    }

    public List<UUID> getContactPreferenceIdsForCase(UUID caseId) {
        return jdbcTemplate.queryForList("""
            SELECT contact_preferences_id FROM party
            WHERE case_id = ? AND contact_preferences_id IS NOT NULL
            """, UUID.class, caseId);
    }

    public List<UUID> getHelpWithFeesIdsForCase(UUID caseId) {
        return jdbcTemplate.queryForList("""
            SELECT hwf_id FROM general_application WHERE case_id = ? AND hwf_id IS NOT NULL
            UNION
            SELECT hwf_id FROM fee_payment
            WHERE possession_claim_id IN (SELECT id FROM claim WHERE case_id = ?)
            AND hwf_id IS NOT NULL
            """, UUID.class, caseId, caseId);
    }

    public List<UUID> getLegalRepresentativeAddressIdsForCase(UUID caseId) {
        return jdbcTemplate.queryForList("""
            SELECT address_id FROM legal_representative
            WHERE id IN (
                SELECT legal_representative_id FROM claim_party_legal_representative
                WHERE party_id IN (SELECT id FROM party WHERE case_id = ?)
            )
            AND address_id IS NOT NULL
            """, UUID.class, caseId);
    }

    public void deleteRowsLinkedToCase(UUID caseId) {
        jdbcTemplate.update("DELETE FROM case_notification WHERE case_id = ?", caseId);
        jdbcTemplate.update("DELETE FROM claim_activity_log WHERE case_id = ?", caseId);
        jdbcTemplate.update("DELETE FROM party_access_code WHERE case_id = ?", caseId);
        jdbcTemplate.update("DELETE FROM case_link_reason WHERE case_link_id IN "
            + "(SELECT id FROM case_link WHERE case_id = ?)", caseId);
        jdbcTemplate.update("DELETE FROM case_link WHERE case_id = ?", caseId);
        jdbcTemplate.update("DELETE FROM case_flag WHERE pcs_case_id = ?", caseId);
        jdbcTemplate.update("DELETE FROM case_note WHERE case_id = ?", caseId);
        jdbcTemplate.update("DELETE FROM document WHERE general_application_id IN "
            + "(SELECT id FROM general_application WHERE case_id = ?)", caseId);
        jdbcTemplate.update("DELETE FROM general_application WHERE case_id = ?", caseId);
        jdbcTemplate.update("DELETE FROM regular_expenses WHERE hc_id IN "
            + "(SELECT id FROM household_circumstances WHERE defendant_response_id IN "
            + DEFENDANT_RESPONSE_IDS_BY_CASE_ID + ")", caseId);
        jdbcTemplate.update("DELETE FROM household_circumstances WHERE defendant_response_id IN "
            + DEFENDANT_RESPONSE_IDS_BY_CASE_ID, caseId);
        jdbcTemplate.update("DELETE FROM reasonable_adjustments WHERE defendant_response_id IN "
            + DEFENDANT_RESPONSE_IDS_BY_CASE_ID, caseId);
        jdbcTemplate.update("DELETE FROM payment_agreement WHERE defendant_response_id IN "
            + DEFENDANT_RESPONSE_IDS_BY_CASE_ID, caseId);
        jdbcTemplate.update("DELETE FROM document WHERE defendant_response_id IN "
            + DEFENDANT_RESPONSE_IDS_BY_CASE_ID, caseId);
        jdbcTemplate.update("DELETE FROM defendant_response WHERE pcs_case_id = ?", caseId);
        jdbcTemplate.update("DELETE FROM document WHERE counter_claim_id IN "
            + "(SELECT id FROM counter_claim WHERE case_id = ?)", caseId);
        jdbcTemplate.update("DELETE FROM counter_claim_party WHERE cc_id IN "
            + "(SELECT id FROM counter_claim WHERE case_id = ?)", caseId);
        jdbcTemplate.update("DELETE FROM counter_claim WHERE case_id = ?", caseId);
        jdbcTemplate.update("DELETE FROM tenancy_licence WHERE case_id = ?", caseId);
    }

    public void deleteRowsLinkedToClaims(UUID caseId) {
        jdbcTemplate.update("DELETE FROM fee_payment WHERE possession_claim_id IN "
            + CLAIM_IDS_BY_CASE_ID, caseId);
        jdbcTemplate.update("DELETE FROM claim_document WHERE claim_id IN "
            + CLAIM_IDS_BY_CASE_ID, caseId);
        jdbcTemplate.update("DELETE FROM claim_ground WHERE claim_id IN "
            + CLAIM_IDS_BY_CASE_ID, caseId);
        jdbcTemplate.update("DELETE FROM notice_of_possession WHERE claim_id IN "
            + CLAIM_IDS_BY_CASE_ID, caseId);
        jdbcTemplate.update("DELETE FROM possession_alternatives WHERE claim_id IN "
            + CLAIM_IDS_BY_CASE_ID, caseId);
        jdbcTemplate.update("DELETE FROM rent_arrears WHERE claim_id IN "
            + CLAIM_IDS_BY_CASE_ID, caseId);
        jdbcTemplate.update("DELETE FROM statement_of_truth WHERE claim_id IN "
            + CLAIM_IDS_BY_CASE_ID, caseId);
        jdbcTemplate.update("DELETE FROM asb_prohibited_conduct WHERE claim_id IN "
            + CLAIM_IDS_BY_CASE_ID, caseId);
        jdbcTemplate.update("DELETE FROM document WHERE enf_case_id IN "
            + "(SELECT id FROM enf_case WHERE claim_id IN " + CLAIM_IDS_BY_CASE_ID + ")", caseId);
        jdbcTemplate.update("DELETE FROM enf_case WHERE claim_id IN "
            + CLAIM_IDS_BY_CASE_ID, caseId);
        jdbcTemplate.update("DELETE FROM document WHERE claim_id IN "
            + CLAIM_IDS_BY_CASE_ID, caseId);
        jdbcTemplate.update("DELETE FROM claim_party WHERE claim_id IN "
            + CLAIM_IDS_BY_CASE_ID, caseId);
    }

    public void deleteRowsLinkedToParties(UUID caseId) {
        jdbcTemplate.update("DELETE FROM case_party_flag WHERE party_id IN "
            + PARTY_IDS_BY_CASE_ID, caseId);
        jdbcTemplate.update("DELETE FROM party_attribute_assertion WHERE party_id IN "
            + PARTY_IDS_BY_CASE_ID, caseId);
        jdbcTemplate.update("DELETE FROM counter_claim_party WHERE party_id IN "
            + PARTY_IDS_BY_CASE_ID, caseId);
        List<UUID> legalRepresentativeIds = jdbcTemplate.queryForList(
            "SELECT legal_representative_id FROM claim_party_legal_representative "
                + "WHERE party_id IN " + PARTY_IDS_BY_CASE_ID,
            UUID.class,
            caseId
        );
        jdbcTemplate.update("DELETE FROM claim_party_legal_representative WHERE party_id IN "
            + PARTY_IDS_BY_CASE_ID, caseId);
        deleteLegalRepresentatives(legalRepresentativeIds);
        jdbcTemplate.update("DELETE FROM document WHERE party_id IN "
            + PARTY_IDS_BY_CASE_ID, caseId);
    }

    public void deleteCaseDocuments(UUID caseId) {
        jdbcTemplate.update("DELETE FROM document WHERE case_id = ?", caseId);
    }

    public void deleteClaims(UUID caseId) {
        jdbcTemplate.update("DELETE FROM claim WHERE case_id = ?", caseId);
    }

    public void deleteParties(UUID caseId) {
        jdbcTemplate.update("DELETE FROM party WHERE case_id = ?", caseId);
    }

    public void deleteCase(UUID caseId) {
        jdbcTemplate.update("DELETE FROM pcs_case WHERE id = ?", caseId);
    }

    public void deleteContactPreferences(List<UUID> contactPreferenceIds) {
        batchDelete("DELETE FROM contact_preferences WHERE id = ?", contactPreferenceIds);
    }

    public void deleteHelpWithFees(List<UUID> helpWithFeesIds) {
        batchDelete("DELETE FROM help_with_fees WHERE id = ?", helpWithFeesIds);
    }

    public void deleteAddresses(List<UUID> addressIds) {
        batchDelete("DELETE FROM address WHERE id = ?", addressIds);
    }

    public void deleteLegalRepresentatives(List<UUID> legalRepresentativeIds) {
        batchDelete("DELETE FROM legal_representative WHERE id = ?", legalRepresentativeIds);
    }

    private void batchDelete(String sql, List<UUID> ids) {
        if (ids.isEmpty()) {
            return;
        }

        jdbcTemplate.batchUpdate(
            sql,
            ids,
            ids.size(),
            (preparedStatement, id) -> preparedStatement.setObject(1, id)
        );
    }
}
