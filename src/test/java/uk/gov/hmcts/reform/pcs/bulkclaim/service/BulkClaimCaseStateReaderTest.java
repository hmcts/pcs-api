package uk.gov.hmcts.reform.pcs.bulkclaim.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import java.sql.ResultSet;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BulkClaimCaseStateReaderTest {

    @Mock
    private JdbcTemplate jdbcTemplate;
    @Mock
    private NamedParameterJdbcTemplate namedParameterJdbcTemplate;
    @InjectMocks
    private BulkClaimCaseStateReader underTest;

    @Test
    void shouldReadStatesInOneQuery() {
        ResultSet row = mock(ResultSet.class);
        doAnswer(invocation -> {
            when(row.getLong("reference")).thenReturn(2L);
            when(row.getString("state")).thenReturn("CASE_ISSUED");
            invocation.<RowCallbackHandler>getArgument(2).processRow(row);
            return null;
        }).when(namedParameterJdbcTemplate).query(anyString(), anyMap(), any(RowCallbackHandler.class));

        Map<Long, String> states = underTest.statesOf(List.of(1L, 2L));

        assertThat(states).containsExactly(Map.entry(2L, "CASE_ISSUED"));
        ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Map<String, Object>> params = ArgumentCaptor.captor();
        verify(namedParameterJdbcTemplate).query(sql.capture(), params.capture(), any(RowCallbackHandler.class));
        assertThat(sql.getValue()).contains("reference IN (:references)");
        assertThat(params.getValue()).containsEntry("references", List.of(1L, 2L));
    }

    @Test
    void shouldReturnNothingWithoutQueryingWhenThereAreNoCases() {
        assertThat(underTest.statesOf(List.of())).isEmpty();

        verifyNoInteractions(jdbcTemplate, namedParameterJdbcTemplate);
    }

    @Test
    void shouldReturnNothingWhenTheQueryFails() {
        doThrow(new DataAccessResourceFailureException("down"))
            .when(namedParameterJdbcTemplate).query(anyString(), anyMap(), any(RowCallbackHandler.class));

        Map<Long, String> states = underTest.statesOf(List.of(1L));

        assertThat(states).isEmpty();
    }
}
