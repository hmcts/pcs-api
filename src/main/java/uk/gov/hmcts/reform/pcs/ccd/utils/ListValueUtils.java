package uk.gov.hmcts.reform.pcs.ccd.utils;

import uk.gov.hmcts.ccd.sdk.type.ListValue;

import java.util.List;

public class ListValueUtils {

    public static <T> List<ListValue<T>> wrapListItems(List<T> items) {
        if (items == null) {
            return List.of();
        }

        return items.stream()
            .map(item -> ListValue.<T>builder().value(item).build())
            .toList();
    }

    public static <T> List<T> unwrapListItems(List<ListValue<T>> items) {
        if (items == null) {
            return List.of();
        }

        return items.stream()
            .map(ListValue::getValue)
            .toList();
    }
}
