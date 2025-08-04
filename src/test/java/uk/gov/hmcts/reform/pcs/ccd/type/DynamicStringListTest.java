package uk.gov.hmcts.reform.pcs.ccd.type;

import org.junit.jupiter.api.Test;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;

class DynamicStringListTest {

    @Test
    void shouldGetValueCodeWhenValueIsNotNull() {
        String expectedValueCode = "code-123";

        DynamicStringListElement selectedValue = DynamicStringListElement.builder()
            .code(expectedValueCode)
            .build();

        DynamicStringList listWithNullSelectedValue = new DynamicStringList(selectedValue, Collections.emptyList());

        assertThat(listWithNullSelectedValue.getValueCode()).isEqualTo(expectedValueCode);
    }

    @Test
    void shouldGetNullValueCodeWhenValueIsNull() {
        DynamicStringList listWithNullSelectedValue = new DynamicStringList(null, Collections.emptyList());

        assertThat(listWithNullSelectedValue.getValueCode()).isNull();
    }

}
