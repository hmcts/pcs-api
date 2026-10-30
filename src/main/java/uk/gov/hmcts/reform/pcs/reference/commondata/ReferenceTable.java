package uk.gov.hmcts.reform.pcs.reference.commondata;

import java.util.Map;

public record ReferenceTable(
    String tableName,
    Map<ReferenceColumn, String> columns,
    boolean tracksModification,
    Map<String, ReferenceRecord> records
) {
}
