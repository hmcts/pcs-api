package uk.gov.hmcts.reform.pcs.ccd.jackson;

import com.fasterxml.jackson.annotation.JsonUnwrapped;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class NestedComplexTypeDeserializerTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void shouldReadNestedFieldsWrittenWithoutThePrefix() throws Exception {
        Parent parent = new Parent(new Tab(new Nested("notice.pdf"), new Nested("tenancy.pdf")));

        String json = mapper.writeValueAsString(parent);
        Parent readBack = mapper.readValue(json, Parent.class);

        assertThat(json).contains("\"tab_withDeserializer\":{\"fileName\":\"notice.pdf\"}");
        assertThat(readBack.getTab().getWithDeserializer().getFileName()).isEqualTo("notice.pdf");
    }

    @Test
    void shouldLoseNestedFieldsWithoutTheDeserializer() throws Exception {
        Parent parent = new Parent(new Tab(null, new Nested("tenancy.pdf")));

        Parent readBack = mapper.readValue(mapper.writeValueAsString(parent), Parent.class);

        assertThat(readBack.getTab().getWithoutDeserializer().getFileName()).isNull();
    }

    @Test
    void shouldReadNullNestedValue() throws Exception {
        Parent readBack = mapper.readValue("{\"tab_withDeserializer\":null}", Parent.class);

        assertThat(readBack.getTab().getWithDeserializer()).isNull();
    }

    @Test
    void shouldRejectUseWithoutAField() {
        assertThatThrownBy(() -> new NestedComplexTypeDeserializer().createContextual(null, null))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("must be used on a field");
    }

    @Test
    void shouldRejectContentUsing() {
        assertThatThrownBy(() -> mapper.readValue("{\"items\":[{\"fileName\":\"a.pdf\"}]}", ContentUsingMisuse.class))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("not 'contentUsing', on items");
    }

    @Data
    @NoArgsConstructor
    static class ContentUsingMisuse {
        @JsonDeserialize(contentUsing = NestedComplexTypeDeserializer.class)
        private List<Nested> items;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    static class Parent {
        @JsonUnwrapped(prefix = "tab_")
        private Tab tab;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    static class Tab {
        @JsonDeserialize(using = NestedComplexTypeDeserializer.class)
        private Nested withDeserializer;

        private Nested withoutDeserializer;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    static class Nested {
        private String fileName;
    }
}
