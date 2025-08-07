package uk.gov.hmcts.reform.pcs.ccd.utils;

import org.junit.jupiter.api.Test;
import uk.gov.hmcts.ccd.sdk.type.ListValue;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class ListValueUtilsTest {

    @Test
    void wrapListItemsReturnsEmptyListWhenInputIsNull() {
        List<ListValue<String>> result = ListValueUtils.wrapListItems(null);
        assertThat(result).isEmpty();
    }

    @Test
    void wrapListItemsWrapsItemsCorrectly() {
        List<String> input = List.of("one", "two", "three");
        List<ListValue<String>> result = ListValueUtils.wrapListItems(input);

        assertThat(result).hasSize(3);
        assertThat(result.get(0).getValue()).isEqualTo("one");
        assertThat(result.get(1).getValue()).isEqualTo("two");
        assertThat(result.get(2).getValue()).isEqualTo("three");
    }

    @Test
    void unwrapListItemsReturnsEmptyListWhenInputIsNull() {
        List<String> result = ListValueUtils.unwrapListItems(null);
        assertThat(result).isEmpty();
    }

    @Test
    void unwrapListItemsUnwrapsValuesCorrectly() {
        List<ListValue<String>> input = List.of(
            ListValue.<String>builder().value("a").build(),
            ListValue.<String>builder().value("b").build()
        );

        List<String> result = ListValueUtils.unwrapListItems(input);

        assertThat(result).containsExactly("a", "b");
    }
}
