package uk.gov.hmcts.reform.pcs.reference.commondata;

import java.util.Set;

public record ReferenceRecord(
    String key,
    String valueEn,
    String valueCy,
    Integer lovOrder,
    String parentCategory,
    String parentKey,
    boolean active
) {

    public ReferenceRecord restrictedTo(Set<ReferenceColumn> columns) {
        return new ReferenceRecord(
            key,
            columns.contains(ReferenceColumn.VALUE_EN) ? valueEn : null,
            columns.contains(ReferenceColumn.VALUE_CY) ? valueCy : null,
            columns.contains(ReferenceColumn.LOV_ORDER) ? lovOrder : null,
            columns.contains(ReferenceColumn.PARENT_CATEGORY) ? parentCategory : null,
            columns.contains(ReferenceColumn.PARENT_KEY) ? parentKey : null,
            active
        );
    }
}
