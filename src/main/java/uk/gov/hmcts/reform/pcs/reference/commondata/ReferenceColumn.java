package uk.gov.hmcts.reform.pcs.reference.commondata;

import java.util.List;
import java.util.function.Function;

public enum ReferenceColumn {

    VALUE_EN(List.of("value_en"), ReferenceRecord::valueEn),
    VALUE_CY(List.of("value_cy"), ReferenceRecord::valueCy),
    LOV_ORDER(List.of("lov_order"), ReferenceRecord::lovOrder),
    PARENT_CATEGORY(List.of("parent_category", "parentcategory"), ReferenceRecord::parentCategory),
    PARENT_KEY(List.of("parent_key", "parentkey"), ReferenceRecord::parentKey);

    private final List<String> candidateNames;
    private final Function<ReferenceRecord, Object> accessor;

    ReferenceColumn(List<String> candidateNames, Function<ReferenceRecord, Object> accessor) {
        this.candidateNames = candidateNames;
        this.accessor = accessor;
    }

    public List<String> candidateNames() {
        return candidateNames;
    }

    public Object extractFrom(ReferenceRecord record) {
        return accessor.apply(record);
    }
}
