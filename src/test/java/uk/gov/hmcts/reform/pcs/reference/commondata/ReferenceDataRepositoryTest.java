package uk.gov.hmcts.reform.pcs.reference.commondata;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ParameterizedPreparedStatementSetter;
import org.springframework.jdbc.core.ResultSetExtractor;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReferenceDataRepositoryTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    private ReferenceDataRepository underTest;

    @BeforeEach
    void setUp() {
        underTest = new ReferenceDataRepository(jdbcTemplate);
    }

    @Test
    void shouldReadRowsUsingOnlyTheColumnsTheTableHas() throws SQLException {
        // given
        ResultSet resultSet = mock(ResultSet.class);
        ResultSetMetaData metaData = mock(ResultSetMetaData.class);
        List<String> columns = List.of("key", "value_en", "parentcategory", "parentkey", "active",
                                       "created_date", "last_modified_date");
        when(resultSet.getMetaData()).thenReturn(metaData);
        when(metaData.getColumnCount()).thenReturn(columns.size());
        for (int index = 0; index < columns.size(); index++) {
            when(metaData.getColumnName(index + 1)).thenReturn(columns.get(index).toUpperCase());
        }
        when(resultSet.next()).thenReturn(true, false);
        when(resultSet.getString("key")).thenReturn("VIDCVP");
        when(resultSet.getString("value_en")).thenReturn("Video - CVP");
        when(resultSet.getString("parentcategory")).thenReturn("HearingChannel");
        when(resultSet.getString("parentkey")).thenReturn("VID");
        when(resultSet.getString("active")).thenReturn("YES");

        // when
        ReferenceTable table = extractWith(ReferenceDataSource.HEARING_SUBCHANNEL, resultSet);

        // then
        assertThat(table.tableName()).isEqualTo("hearing_subchannel");
        assertThat(table.tracksModification()).isTrue();
        assertThat(table.columns()).containsOnlyKeys(
            ReferenceColumn.VALUE_EN, ReferenceColumn.PARENT_CATEGORY, ReferenceColumn.PARENT_KEY);
        assertThat(table.records()).containsOnlyKeys("VIDCVP");
        assertThat(table.records().get("VIDCVP")).isEqualTo(
            new ReferenceRecord("VIDCVP", "Video - CVP", null, null, "HearingChannel", "VID", true));
    }

    @Test
    void shouldReadInactiveRowsAndTablesWithoutModificationTracking() throws SQLException {
        // given
        ResultSet resultSet = mock(ResultSet.class);
        ResultSetMetaData metaData = mock(ResultSetMetaData.class);
        List<String> columns = List.of("key", "value_en", "lov_order", "active");
        when(resultSet.getMetaData()).thenReturn(metaData);
        when(metaData.getColumnCount()).thenReturn(columns.size());
        for (int index = 0; index < columns.size(); index++) {
            when(metaData.getColumnName(index + 1)).thenReturn(columns.get(index));
        }
        when(resultSet.next()).thenReturn(true, false);
        when(resultSet.getString("key")).thenReturn("A");
        when(resultSet.getString("value_en")).thenReturn("Alpha");
        when(resultSet.getObject("lov_order", Integer.class)).thenReturn(4);
        when(resultSet.getString("active")).thenReturn("NO");

        // when
        ReferenceTable table = extractWith(ReferenceDataSource.HEARING_TYPE, resultSet);

        // then
        assertThat(table.tracksModification()).isFalse();
        assertThat(table.records().get("A")).isEqualTo(new ReferenceRecord("A", "Alpha", null, 4, null, null, false));
    }

    @Test
    void shouldInsertNewRowsIdempotentlyWithTheTableColumns() throws SQLException {
        // given
        ReferenceTable table = table(true, ReferenceColumn.VALUE_EN, ReferenceColumn.VALUE_CY);
        ReferenceRecord record = new ReferenceRecord("A", "Alpha", "Alffa", null, null, null, true);

        // when
        underTest.apply(table, List.of(record), List.of());

        // then
        ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<ParameterizedPreparedStatementSetter<ReferenceRecord>> setter = setterCaptor();
        verify(jdbcTemplate).batchUpdate(sql.capture(), eq(List.of(record)), eq(1), setter.capture());
        assertThat(sql.getValue()).isEqualTo(
            "INSERT INTO public.hearing_type (key, value_en, value_cy, active) VALUES (?, ?, ?, ?::public.yes_no) "
                + "ON CONFLICT (key) DO NOTHING");

        PreparedStatement statement = mock(PreparedStatement.class);
        setter.getValue().setValues(statement, record);
        var ordered = inOrder(statement);
        ordered.verify(statement).setString(1, "A");
        ordered.verify(statement).setObject(2, "Alpha");
        ordered.verify(statement).setObject(3, "Alffa");
        ordered.verify(statement).setString(4, "YES");
    }

    @Test
    void shouldUpdateOnlyTheKeyedRowAndStampModificationTime() throws SQLException {
        // given
        ReferenceTable table = table(true, ReferenceColumn.VALUE_EN, ReferenceColumn.PARENT_KEY);
        ReferenceRecord record = new ReferenceRecord("A", "Alpha", null, null, null, "P", false);

        // when
        underTest.apply(table, List.of(), List.of(record));

        // then
        ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<ParameterizedPreparedStatementSetter<ReferenceRecord>> setter = setterCaptor();
        verify(jdbcTemplate).batchUpdate(sql.capture(), eq(List.of(record)), eq(1), setter.capture());
        assertThat(sql.getValue()).isEqualTo(
            "UPDATE public.hearing_type SET value_en = ?, parent_key = ?, active = ?::public.yes_no, "
                + "last_modified_date = CURRENT_TIMESTAMP WHERE key = ?");

        PreparedStatement statement = mock(PreparedStatement.class);
        setter.getValue().setValues(statement, record);
        var ordered = inOrder(statement);
        ordered.verify(statement).setObject(1, "Alpha");
        ordered.verify(statement).setObject(2, "P");
        ordered.verify(statement).setString(3, "NO");
        ordered.verify(statement).setString(4, "A");
    }

    @Test
    void shouldNotStampModificationTimeWhenTheTableDoesNotTrackIt() {
        // given
        ReferenceTable table = table(false, ReferenceColumn.VALUE_EN);
        ReferenceRecord record = new ReferenceRecord("A", "Alpha", null, null, null, null, true);

        // when
        underTest.apply(table, List.of(), List.of(record));

        // then
        ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
        verify(jdbcTemplate).batchUpdate(sql.capture(), anyRecords(), anyInt(), anySetter());
        assertThat(sql.getValue()).doesNotContain("last_modified_date");
    }

    @Test
    void shouldNotIssueStatementsWhenThereIsNothingToWrite() {
        // when
        underTest.apply(table(true, ReferenceColumn.VALUE_EN), List.of(), List.of());

        // then
        verify(jdbcTemplate, never()).batchUpdate(anyString(), anyRecords(), anyInt(), anySetter());
    }

    private Collection<ReferenceRecord> anyRecords() {
        return ArgumentMatchers.<ReferenceRecord>anyCollection();
    }

    private ParameterizedPreparedStatementSetter<ReferenceRecord> anySetter() {
        return ArgumentMatchers.<ParameterizedPreparedStatementSetter<ReferenceRecord>>any();
    }

    private ReferenceTable extractWith(ReferenceDataSource source, ResultSet resultSet) throws SQLException {
        @SuppressWarnings("unchecked")
        ArgumentCaptor<ResultSetExtractor<ReferenceTable>> extractor =
            ArgumentCaptor.forClass(ResultSetExtractor.class);
        underTest.load(source);
        verify(jdbcTemplate).query(eq("SELECT * FROM public." + source.getTableName()), extractor.capture());
        return extractor.getValue().extractData(resultSet);
    }

    @SuppressWarnings("unchecked")
    private ArgumentCaptor<ParameterizedPreparedStatementSetter<ReferenceRecord>> setterCaptor() {
        return ArgumentCaptor.forClass(ParameterizedPreparedStatementSetter.class);
    }

    private ReferenceTable table(boolean tracksModification, ReferenceColumn... columns) {
        Map<ReferenceColumn, String> names = new LinkedHashMap<>();
        for (ReferenceColumn column : columns) {
            names.put(column, column.candidateNames().get(0));
        }
        return new ReferenceTable("hearing_type", names, tracksModification, Map.of());
    }
}
