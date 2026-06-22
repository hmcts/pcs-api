package uk.gov.hmcts.reform.pcs.ccd.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CcdCaseDataServiceTest {

    private static final long CASE_REFERENCE = 1234L;

    private final JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);

    private CcdCaseDataService underTest;

    @BeforeEach
    void setUp() {
        underTest = new CcdCaseDataService(jdbcTemplate);
    }

    @Test
    void shouldReturnTrueWhenCaseIsDeleted() {
        when(jdbcTemplate.queryForObject(
            "SELECT state FROM ccd.case_data WHERE reference = ?",
            String.class,
            CASE_REFERENCE
        )).thenReturn("DELETED");

        assertThat(underTest.isCaseDeletedOrMissing(CASE_REFERENCE)).isTrue();
    }

    @Test
    void shouldReturnTrueWhenCaseIsMissing() {
        when(jdbcTemplate.queryForObject(
            "SELECT state FROM ccd.case_data WHERE reference = ?",
            String.class,
            CASE_REFERENCE
        )).thenThrow(new EmptyResultDataAccessException(1));

        assertThat(underTest.isCaseDeletedOrMissing(CASE_REFERENCE)).isTrue();
    }

    @Test
    void shouldReturnFalseWhenCaseIsNotDeleted() {
        when(jdbcTemplate.queryForObject(
            "SELECT state FROM ccd.case_data WHERE reference = ?",
            String.class,
            CASE_REFERENCE
        )).thenReturn("AWAITING_SUBMISSION_TO_HMCTS");

        assertThat(underTest.isCaseDeletedOrMissing(CASE_REFERENCE)).isFalse();
    }
}
