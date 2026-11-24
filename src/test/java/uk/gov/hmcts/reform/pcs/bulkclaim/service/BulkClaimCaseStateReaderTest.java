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

import java.sql.ResultSet;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
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
    @InjectMocks
    private BulkClaimCaseStateReader underTest;

    @Test
    void shouldReadStatesInOneQuery() {
        ResultSet row = mock(ResultSet.class);
        doAnswer(invocation -> {
            when(row.getLong("reference")).thenReturn(2L);
            when(row.getString("state")).thenReturn("CASE_ISSUED");
            invocation.<RowCallbackHandler>getArgument(1).processRow(row);
            return null;
        }).when(jdbcTemplate).query(anyString(), any(RowCallbackHandler.class), any(Object[].class));

        Map<Long, String> states = underTest.statesOf(List.of(1L, 2L));

        assertThat(states).containsExactly(Map.entry(2L, "CASE_ISSUED"));
        ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Object[]> args = ArgumentCaptor.forClass(Object[].class);
        verify(jdbcTemplate).query(sql.capture(), any(RowCallbackHandler.class), args.capture());
        assertThat(sql.getValue()).contains("reference IN (?,?)");
        assertThat(args.getValue()).containsExactly(1L, 2L);
    }

    @Test
    void shouldReturnNothingWithoutQueryingWhenThereAreNoCases() {
        assertThat(underTest.statesOf(List.of())).isEmpty();

        verifyNoInteractions(jdbcTemplate);
    }

    @Test
    void shouldReturnNothingWhenTheQueryFails() {
        doThrow(new DataAccessResourceFailureException("down"))
            .when(jdbcTemplate).query(anyString(), any(RowCallbackHandler.class), any(Object[].class));

        Map<Long, String> states = underTest.statesOf(List.of(1L));

        assertThat(states).isEmpty();
    }
}
