package uk.gov.hmcts.reform.pcs.ccd.service;

import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CaseReindexServiceTest {

    private static final long CASE_REFERENCE = 1787849985141941L;

    @Mock
    private NamedParameterJdbcTemplate jdbcTemplate;

    private CaseReindexService underTest;

    @BeforeEach
    void setUp() {
        underTest = new CaseReindexService(jdbcTemplate);
    }

    @Test
    void shouldQueueTheCaseOnTheReindexQueue() {
        when(jdbcTemplate.update(anyString(), anyMap())).thenReturn(1);

        underTest.reindex(CASE_REFERENCE);

        ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, Object>> params = ArgumentCaptor.forClass(Map.class);
        verify(jdbcTemplate).update(sql.capture(), params.capture());

        assertThat(sql.getValue()).contains("insert into ccd.es_queue");
        assertThat(sql.getValue()).contains("on conflict (reference) do update");
        assertThat(params.getValue()).containsEntry("reference", CASE_REFERENCE);
    }
}
