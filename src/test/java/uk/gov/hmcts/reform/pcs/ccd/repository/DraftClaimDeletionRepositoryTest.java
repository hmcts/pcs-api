package uk.gov.hmcts.reform.pcs.ccd.repository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DraftClaimDeletionRepositoryTest {

    private static final long CASE_REFERENCE = 1234567890123456L;
    private static final UUID CASE_ID = UUID.randomUUID();
    private static final UUID LEGAL_REPRESENTATIVE_ID = UUID.randomUUID();

    @Mock
    private JdbcTemplate jdbcTemplate;

    private DraftClaimDeletionRepository underTest;

    @BeforeEach
    void setUp() {
        underTest = new DraftClaimDeletionRepository(jdbcTemplate);
    }

    @Test
    void shouldReturnNullWhenCaseIdDoesNotExist() {
        when(jdbcTemplate.queryForObject(
            "SELECT id FROM pcs_case WHERE case_reference = ?",
            UUID.class,
            CASE_REFERENCE
        )).thenThrow(new EmptyResultDataAccessException(1));

        assertThat(underTest.getCaseId(CASE_REFERENCE)).isNull();
    }

    @Test
    void shouldTreatNullIssuedClaimResultAsNotIssued() {
        when(jdbcTemplate.queryForObject(
            "SELECT EXISTS (SELECT 1 FROM claim WHERE case_id = ? AND claim_issued_date IS NOT NULL)",
            Boolean.class,
            CASE_ID
        )).thenReturn(null);

        assertThat(underTest.hasIssuedClaim(CASE_ID)).isFalse();
    }

    @Test
    void shouldLockClaimRowsForCase() {
        underTest.lockClaimsForCase(CASE_ID);

        verify(jdbcTemplate).queryForList(
            "SELECT id FROM claim WHERE case_id = ? FOR UPDATE",
            UUID.class,
            CASE_ID
        );
    }

    @Test
    void shouldDeletePartyLinkedRowsAndLegalRepresentatives() {
        when(jdbcTemplate.queryForList(
            "SELECT legal_representative_id FROM claim_party_legal_representative "
                + "WHERE party_id IN (SELECT id FROM party WHERE case_id = ?)",
            UUID.class,
            CASE_ID
        )).thenReturn(List.of(LEGAL_REPRESENTATIVE_ID));

        underTest.deleteRowsLinkedToParties(CASE_ID);

        verify(jdbcTemplate).batchUpdate(
            eq("DELETE FROM legal_representative WHERE id = ?"),
            eq(List.of(LEGAL_REPRESENTATIVE_ID)),
            eq(1),
            any()
        );
        verify(jdbcTemplate).update(
            "DELETE FROM claim_party_legal_representative WHERE party_id IN "
                + "(SELECT id FROM party WHERE case_id = ?)",
            CASE_ID
        );
    }

    @Test
    void shouldBatchDeleteAddresses() {
        UUID addressId = UUID.randomUUID();

        underTest.deleteAddresses(List.of(addressId));

        verify(jdbcTemplate).batchUpdate(eq("DELETE FROM address WHERE id = ?"), eq(List.of(addressId)), eq(1), any());
    }

    @Test
    void shouldNotBatchDeleteWhenIdListIsEmpty() {
        underTest.deleteAddresses(List.of());

        verify(jdbcTemplate, never()).batchUpdate(eq("DELETE FROM address WHERE id = ?"), any(), eq(0), any());
    }
}
