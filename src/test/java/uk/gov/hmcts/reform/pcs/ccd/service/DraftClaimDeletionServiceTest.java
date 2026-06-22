package uk.gov.hmcts.reform.pcs.ccd.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DraftClaimDeletionServiceTest {

    private static final long CASE_REFERENCE = 1234567890123456L;
    private static final UUID CASE_ID = UUID.randomUUID();
    private static final UUID ADDRESS_ID = UUID.randomUUID();
    private static final UUID CONTACT_PREFERENCE_ID = UUID.randomUUID();
    private static final UUID HELP_WITH_FEES_ID = UUID.randomUUID();
    private static final UUID LEGAL_REPRESENTATIVE_ID = UUID.randomUUID();
    private static final UUID LEGAL_REPRESENTATIVE_ADDRESS_ID = UUID.randomUUID();
    private static final String GET_ADDRESS_IDS = """
        SELECT property_address_id FROM pcs_case WHERE id = ? AND property_address_id IS NOT NULL
        UNION
        SELECT address_id FROM party WHERE case_id = ? AND address_id IS NOT NULL
        """;
    private static final String GET_CONTACT_PREFERENCE_IDS = """
        SELECT contact_preferences_id FROM party
        WHERE case_id = ? AND contact_preferences_id IS NOT NULL
        """;
    private static final String GET_LEGAL_REPRESENTATIVE_IDS =
        "SELECT legal_representative_id FROM claim_party_legal_representative "
            + "WHERE party_id IN (SELECT id FROM party WHERE case_id = ?)";
    private static final String GET_HELP_WITH_FEES_IDS = """
        SELECT hwf_id FROM general_application WHERE case_id = ? AND hwf_id IS NOT NULL
        UNION
        SELECT hwf_id FROM fee_payment
        WHERE possession_claim_id IN (SELECT id FROM claim WHERE case_id = ?)
        AND hwf_id IS NOT NULL
        """;
    private static final String GET_LEGAL_REPRESENTATIVE_ADDRESS_IDS = """
        SELECT address_id FROM legal_representative
        WHERE id IN (
            SELECT legal_representative_id FROM claim_party_legal_representative
            WHERE party_id IN (SELECT id FROM party WHERE case_id = ?)
        )
        AND address_id IS NOT NULL
        """;

    @Mock
    private JdbcTemplate jdbcTemplate;

    private DraftClaimDeletionService underTest;

    @BeforeEach
    void setUp() {
        underTest = new DraftClaimDeletionService(jdbcTemplate);
    }

    @Test
    void shouldDeleteDraftDataWhenCaseRecordIsAlreadyAbsent() {
        when(jdbcTemplate.queryForObject(
            "SELECT id FROM pcs_case WHERE case_reference = ?",
            UUID.class,
            CASE_REFERENCE
        )).thenThrow(new EmptyResultDataAccessException(1));

        underTest.deleteDraftClaim(CASE_REFERENCE);

        verify(jdbcTemplate).update("DELETE FROM draft.draft_case_data WHERE case_reference = ?", CASE_REFERENCE);
        verify(jdbcTemplate).update("DELETE FROM ccd.case_data WHERE reference = ?", CASE_REFERENCE);
        verify(jdbcTemplate, never()).update("DELETE FROM pcs_case WHERE id = ?", CASE_ID);
    }

    @Test
    void shouldRejectDeletionWhenClaimHasBeenIssued() {
        when(jdbcTemplate.queryForObject(
            "SELECT id FROM pcs_case WHERE case_reference = ?",
            UUID.class,
            CASE_REFERENCE
        )).thenReturn(CASE_ID);
        when(jdbcTemplate.queryForObject(
            "SELECT EXISTS (SELECT 1 FROM claim WHERE case_id = ? AND claim_issued_date IS NOT NULL)",
            Boolean.class,
            CASE_ID
        )).thenReturn(true);

        assertThatThrownBy(() -> underTest.deleteDraftClaim(CASE_REFERENCE))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("Cannot delete a claim that has been issued");

        verify(jdbcTemplate, never())
            .update("DELETE FROM draft.draft_case_data WHERE case_reference = ?", CASE_REFERENCE);
        verify(jdbcTemplate, never()).update("DELETE FROM ccd.case_data WHERE reference = ?", CASE_REFERENCE);
        verify(jdbcTemplate, never()).update("DELETE FROM pcs_case WHERE id = ?", CASE_ID);
        verify(jdbcTemplate, never()).queryForList(anyString(), eq(UUID.class), eq(CASE_ID), eq(CASE_ID), eq(CASE_ID));
    }

    @Test
    void shouldDeleteDraftClaimAndLinkedDataWhenClaimHasNotBeenIssued() {
        when(jdbcTemplate.queryForObject(
            "SELECT id FROM pcs_case WHERE case_reference = ?",
            UUID.class,
            CASE_REFERENCE
        )).thenReturn(CASE_ID);
        when(jdbcTemplate.queryForObject(
            "SELECT EXISTS (SELECT 1 FROM claim WHERE case_id = ? AND claim_issued_date IS NOT NULL)",
            Boolean.class,
            CASE_ID
        )).thenReturn(false);
        when(jdbcTemplate.queryForList(GET_ADDRESS_IDS, UUID.class, CASE_ID, CASE_ID))
            .thenReturn(List.of(ADDRESS_ID));
        when(jdbcTemplate.queryForList(GET_CONTACT_PREFERENCE_IDS, UUID.class, CASE_ID))
            .thenReturn(List.of(CONTACT_PREFERENCE_ID));
        when(jdbcTemplate.queryForList(GET_HELP_WITH_FEES_IDS, UUID.class, CASE_ID, CASE_ID))
            .thenReturn(List.of(HELP_WITH_FEES_ID));
        when(jdbcTemplate.queryForList(GET_LEGAL_REPRESENTATIVE_ADDRESS_IDS, UUID.class, CASE_ID))
            .thenReturn(List.of(LEGAL_REPRESENTATIVE_ADDRESS_ID));
        when(jdbcTemplate.queryForList(GET_LEGAL_REPRESENTATIVE_IDS, UUID.class, CASE_ID))
            .thenReturn(List.of(LEGAL_REPRESENTATIVE_ID));

        underTest.deleteDraftClaim(CASE_REFERENCE);

        verify(jdbcTemplate).update("DELETE FROM draft.draft_case_data WHERE case_reference = ?", CASE_REFERENCE);
        verify(jdbcTemplate).update("DELETE FROM pcs_case WHERE id = ?", CASE_ID);
        verify(jdbcTemplate).update("DELETE FROM contact_preferences WHERE id = ?", CONTACT_PREFERENCE_ID);
        verify(jdbcTemplate).update("DELETE FROM help_with_fees WHERE id = ?", HELP_WITH_FEES_ID);
        verify(jdbcTemplate).update("DELETE FROM address WHERE id = ?", LEGAL_REPRESENTATIVE_ADDRESS_ID);
        verify(jdbcTemplate).update("DELETE FROM address WHERE id = ?", ADDRESS_ID);
        verify(jdbcTemplate).update("DELETE FROM legal_representative WHERE id = ?", LEGAL_REPRESENTATIVE_ID);
        verify(jdbcTemplate).update("DELETE FROM ccd.case_data WHERE reference = ?", CASE_REFERENCE);
    }

    @Test
    void shouldTreatNullIssuedClaimResultAsNotIssued() {
        when(jdbcTemplate.queryForObject(
            "SELECT id FROM pcs_case WHERE case_reference = ?",
            UUID.class,
            CASE_REFERENCE
        )).thenReturn(CASE_ID);
        when(jdbcTemplate.queryForObject(
            "SELECT EXISTS (SELECT 1 FROM claim WHERE case_id = ? AND claim_issued_date IS NOT NULL)",
            Boolean.class,
            CASE_ID
        )).thenReturn(null);
        when(jdbcTemplate.queryForList(GET_ADDRESS_IDS, UUID.class, CASE_ID, CASE_ID))
            .thenReturn(List.of());
        when(jdbcTemplate.queryForList(GET_CONTACT_PREFERENCE_IDS, UUID.class, CASE_ID))
            .thenReturn(List.of());
        when(jdbcTemplate.queryForList(GET_HELP_WITH_FEES_IDS, UUID.class, CASE_ID, CASE_ID))
            .thenReturn(List.of());
        when(jdbcTemplate.queryForList(GET_LEGAL_REPRESENTATIVE_ADDRESS_IDS, UUID.class, CASE_ID))
            .thenReturn(List.of());
        when(jdbcTemplate.queryForList(GET_LEGAL_REPRESENTATIVE_IDS, UUID.class, CASE_ID))
            .thenReturn(List.of());

        underTest.deleteDraftClaim(CASE_REFERENCE);

        verify(jdbcTemplate).update("DELETE FROM pcs_case WHERE id = ?", CASE_ID);
    }
}
