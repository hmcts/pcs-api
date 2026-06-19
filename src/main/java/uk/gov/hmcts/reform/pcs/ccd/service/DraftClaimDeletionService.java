package uk.gov.hmcts.reform.pcs.ccd.service;

import lombok.AllArgsConstructor;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@AllArgsConstructor
public class DraftClaimDeletionService {

    private static final String DEFENDANT_RESPONSE_IDS_BY_CASE_ID =
        "(SELECT id FROM defendant_response WHERE pcs_case_id = ?)";
    private static final String CLAIM_IDS_BY_CASE_ID = "(SELECT id FROM claim WHERE case_id = ?)";
    private static final String PARTY_IDS_BY_CASE_ID = "(SELECT id FROM party WHERE case_id = ?)";

    private final JdbcTemplate jdbcTemplate;

    @Transactional
    public void deleteDraftClaim(long caseReference) {
        UUID caseId = getCaseId(caseReference);

        if (caseId == null) {
            jdbcTemplate.update("DELETE FROM draft.draft_case_data WHERE case_reference = ?", caseReference);
            deleteCcdCaseData(caseReference);
            return;
        }

        if (hasIssuedClaim(caseId)) {
            throw new IllegalStateException("Cannot delete a claim that has been issued");
        }

        jdbcTemplate.update("DELETE FROM draft.draft_case_data WHERE case_reference = ?", caseReference);

        final List<UUID> addressIds = getAddressIdsForCase(caseId);
        final List<UUID> contactPreferenceIds = getContactPreferenceIdsForCase(caseId);

        deleteRowsLinkedToCase(caseId);
        deleteRowsLinkedToClaims(caseId);
        deleteRowsLinkedToParties(caseId);

        jdbcTemplate.update("DELETE FROM document WHERE case_id = ?", caseId);
        jdbcTemplate.update("DELETE FROM claim WHERE case_id = ?", caseId);
        jdbcTemplate.update("DELETE FROM party WHERE case_id = ?", caseId);
        jdbcTemplate.update("DELETE FROM pcs_case WHERE id = ?", caseId);

        contactPreferenceIds.forEach(contactPreferenceId ->
            jdbcTemplate.update("DELETE FROM contact_preferences WHERE id = ?", contactPreferenceId));
        addressIds.forEach(addressId -> jdbcTemplate.update("DELETE FROM address WHERE id = ?", addressId));

        deleteCcdCaseData(caseReference);
    }

    private void deleteCcdCaseData(long caseReference) {
        jdbcTemplate.update("DELETE FROM ccd.case_data WHERE reference = ?", caseReference);
    }

    private UUID getCaseId(long caseReference) {
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

    private boolean hasIssuedClaim(UUID caseId) {
        Boolean result = jdbcTemplate.queryForObject(
            "SELECT EXISTS (SELECT 1 FROM claim WHERE case_id = ? AND claim_issued_date IS NOT NULL)",
            Boolean.class,
            caseId
        );
        return Boolean.TRUE.equals(result);
    }

    private List<UUID> getAddressIdsForCase(UUID caseId) {
        return jdbcTemplate.queryForList("""
            SELECT property_address_id FROM pcs_case WHERE id = ? AND property_address_id IS NOT NULL
            UNION
            SELECT address_id FROM party WHERE case_id = ? AND address_id IS NOT NULL
            """, UUID.class, caseId, caseId);
    }

    private List<UUID> getContactPreferenceIdsForCase(UUID caseId) {
        return jdbcTemplate.queryForList("""
            SELECT contact_preferences_id FROM party
            WHERE case_id = ? AND contact_preferences_id IS NOT NULL
            """, UUID.class, caseId);
    }

    private void deleteRowsLinkedToCase(UUID caseId) {
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

    private void deleteRowsLinkedToClaims(UUID caseId) {
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

    private void deleteRowsLinkedToParties(UUID caseId) {
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
        legalRepresentativeIds.forEach(legalRepresentativeId ->
            jdbcTemplate.update("DELETE FROM legal_representative WHERE id = ?", legalRepresentativeId));
        jdbcTemplate.update("DELETE FROM document WHERE party_id IN "
            + PARTY_IDS_BY_CASE_ID, caseId);
    }
}
