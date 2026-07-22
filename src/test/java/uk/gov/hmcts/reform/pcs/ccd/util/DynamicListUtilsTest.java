package uk.gov.hmcts.reform.pcs.ccd.util;

import org.junit.jupiter.api.Test;
import uk.gov.hmcts.ccd.sdk.type.DynamicListElement;
import uk.gov.hmcts.reform.pcs.ccd.type.DynamicStringListElement;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.hmcts.reform.pcs.ccd.util.DynamicListUtils.retainSelectedValue;

class DynamicListUtilsTest {

    private static final UUID SELECTED_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID OTHER_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");

    @Test
    void shouldRetainDynamicListSelectionByCode() {
        DynamicListElement selected = dynamicListElement(SELECTED_ID, "Old label");
        DynamicListElement matchingOption = dynamicListElement(SELECTED_ID, "New label");

        DynamicListElement result = retainSelectedValue(
            selected,
            List.of(dynamicListElement(OTHER_ID, "Other"), matchingOption)
        );

        assertThat(result).isSameAs(matchingOption);
    }

    @Test
    void shouldRetainDynamicListSelectionByLabelWhenCodeIsMissing() {
        DynamicListElement selected = DynamicListElement.builder()
            .label("Selected label")
            .build();
        DynamicListElement matchingOption = dynamicListElement(SELECTED_ID, "Selected label");

        DynamicListElement result = retainSelectedValue(
            selected,
            List.of(dynamicListElement(OTHER_ID, "Other"), matchingOption)
        );

        assertThat(result).isSameAs(matchingOption);
    }

    @Test
    void shouldReturnNullWhenDynamicListSelectionIsBlankOrNotPresent() {
        assertThat(retainSelectedValue((DynamicListElement) null, List.of())).isNull();
        assertThat(retainSelectedValue(DynamicListElement.builder().build(), List.of())).isNull();
        assertThat(retainSelectedValue(
            dynamicListElement(SELECTED_ID, "Missing"),
            List.of(dynamicListElement(OTHER_ID, "Other"))
        )).isNull();
    }

    @Test
    void shouldRetainDynamicStringListSelectionByCode() {
        DynamicStringListElement selected = dynamicStringListElement("selected-code", "Old label");
        DynamicStringListElement matchingOption = dynamicStringListElement("selected-code", "New label");

        DynamicStringListElement result = retainSelectedValue(
            selected,
            List.of(dynamicStringListElement("other-code", "Other"), matchingOption)
        );

        assertThat(result).isSameAs(matchingOption);
    }

    @Test
    void shouldRetainDynamicStringListSelectionByLabelWhenCodeIsMissing() {
        DynamicStringListElement selected = DynamicStringListElement.builder()
            .label("Selected label")
            .build();
        DynamicStringListElement matchingOption = dynamicStringListElement("selected-code", "Selected label");

        DynamicStringListElement result = retainSelectedValue(
            selected,
            List.of(dynamicStringListElement("other-code", "Other"), matchingOption)
        );

        assertThat(result).isSameAs(matchingOption);
    }

    @Test
    void shouldReturnNullWhenDynamicStringListSelectionIsBlankOrNotPresent() {
        assertThat(retainSelectedValue((DynamicStringListElement) null, List.of())).isNull();
        assertThat(retainSelectedValue(DynamicStringListElement.builder().build(), List.of())).isNull();
        assertThat(retainSelectedValue(
            dynamicStringListElement("selected-code", "Missing"),
            List.of(dynamicStringListElement("other-code", "Other"))
        )).isNull();
    }

    private static DynamicListElement dynamicListElement(UUID code, String label) {
        return DynamicListElement.builder()
            .code(code)
            .label(label)
            .build();
    }

    private static DynamicStringListElement dynamicStringListElement(String code, String label) {
        return DynamicStringListElement.builder()
            .code(code)
            .label(label)
            .build();
    }
}
