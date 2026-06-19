package uk.gov.hmcts.reform.pcs.ccd.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;

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
        verify(jdbcTemplate, never()).update(eq("DELETE FROM pcs_case WHERE id = ?"), eq(CASE_ID));
        verify(jdbcTemplate, never()).queryForList(anyString(), eq(UUID.class), eq(CASE_ID), eq(CASE_ID), eq(CASE_ID));
    }
}
