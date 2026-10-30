package uk.gov.hmcts.reform.pcs.reference.commondata;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Repository
@RequiredArgsConstructor
public class ReferenceDataRepository {

    private static final String KEY_COLUMN = "key";
    private static final String ACTIVE_COLUMN = "active";
    private static final String LAST_MODIFIED_COLUMN = "last_modified_date";
    private static final String YES = "YES";
    private static final String NO = "NO";

    private final JdbcTemplate jdbcTemplate;

    public ReferenceTable load(ReferenceDataSource source) {
        ResultSetExtractor<ReferenceTable> extractor = resultSet -> readTable(source, resultSet);
        return jdbcTemplate.query("SELECT * FROM public." + source.getTableName(), extractor);
    }

    @Transactional
    public void apply(ReferenceTable table, List<ReferenceRecord> inserts, List<ReferenceRecord> updates) {
        if (!inserts.isEmpty()) {
            jdbcTemplate.batchUpdate(insertSql(table), inserts, inserts.size(),
                                     (statement, record) -> bind(statement, table, record, false));
        }
        if (!updates.isEmpty()) {
            jdbcTemplate.batchUpdate(updateSql(table), updates, updates.size(),
                                     (statement, record) -> bind(statement, table, record, true));
        }
    }

    private ReferenceTable readTable(ReferenceDataSource source, ResultSet resultSet) throws SQLException {
        List<String> available = columnNames(resultSet.getMetaData());
        Map<ReferenceColumn, String> columns = resolveColumns(available);

        Map<String, ReferenceRecord> records = new LinkedHashMap<>();
        while (resultSet.next()) {
            ReferenceRecord record = readRecord(resultSet, columns);
            records.put(record.key(), record);
        }
        return new ReferenceTable(
            source.getTableName(),
            columns,
            available.contains(LAST_MODIFIED_COLUMN),
            records
        );
    }

    private ReferenceRecord readRecord(ResultSet resultSet, Map<ReferenceColumn, String> columns) throws SQLException {
        return new ReferenceRecord(
            resultSet.getString(KEY_COLUMN),
            readString(resultSet, columns, ReferenceColumn.VALUE_EN),
            readString(resultSet, columns, ReferenceColumn.VALUE_CY),
            columns.containsKey(ReferenceColumn.LOV_ORDER)
                ? resultSet.getObject(columns.get(ReferenceColumn.LOV_ORDER), Integer.class) : null,
            readString(resultSet, columns, ReferenceColumn.PARENT_CATEGORY),
            readString(resultSet, columns, ReferenceColumn.PARENT_KEY),
            YES.equals(resultSet.getString(ACTIVE_COLUMN))
        );
    }

    private String readString(ResultSet resultSet, Map<ReferenceColumn, String> columns, ReferenceColumn column)
        throws SQLException {
        return columns.containsKey(column) ? resultSet.getString(columns.get(column)) : null;
    }

    private Map<ReferenceColumn, String> resolveColumns(List<String> available) {
        Map<ReferenceColumn, String> columns = new LinkedHashMap<>();
        for (ReferenceColumn column : ReferenceColumn.values()) {
            column.candidateNames().stream()
                .filter(available::contains)
                .findFirst()
                .ifPresent(name -> columns.put(column, name));
        }
        return columns;
    }

    private List<String> columnNames(ResultSetMetaData metaData) throws SQLException {
        List<String> names = new ArrayList<>();
        for (int index = 1; index <= metaData.getColumnCount(); index++) {
            names.add(metaData.getColumnName(index).toLowerCase(Locale.ROOT));
        }
        return names;
    }

    private String insertSql(ReferenceTable table) {
        List<String> columnNames = new ArrayList<>(List.of(KEY_COLUMN));
        columnNames.addAll(table.columns().values());
        columnNames.add(ACTIVE_COLUMN);
        List<String> placeholders = columnNames.stream()
            .map(name -> ACTIVE_COLUMN.equals(name) ? "?::public.yes_no" : "?")
            .toList();
        return "INSERT INTO public." + table.tableName() + " (" + String.join(", ", columnNames) + ") VALUES ("
            + String.join(", ", placeholders) + ") ON CONFLICT (key) DO NOTHING";
    }

    private String updateSql(ReferenceTable table) {
        List<String> assignments = new ArrayList<>();
        table.columns().values().forEach(name -> assignments.add(name + " = ?"));
        assignments.add(ACTIVE_COLUMN + " = ?::public.yes_no");
        if (table.tracksModification()) {
            assignments.add(LAST_MODIFIED_COLUMN + " = CURRENT_TIMESTAMP");
        }
        return "UPDATE public." + table.tableName() + " SET " + String.join(", ", assignments) + " WHERE key = ?";
    }

    private void bind(PreparedStatement statement, ReferenceTable table, ReferenceRecord record, boolean update)
        throws SQLException {
        int index = 1;
        if (!update) {
            statement.setString(index++, record.key());
        }
        for (ReferenceColumn column : table.columns().keySet()) {
            statement.setObject(index++, column.extractFrom(record));
        }
        statement.setString(index++, record.active() ? YES : NO);
        if (update) {
            statement.setString(index, record.key());
        }
    }
}
