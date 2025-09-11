package uk.gov.hmcts.reform.pcs.ccd3.type;

import org.junit.jupiter.api.Test;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class DynamicStringListTest {

    @Test
    void shouldCreateDynamicStringListWithNoArgsConstructor() {
        // When
        DynamicStringList list = new DynamicStringList();

        // Then
        assertThat(list).isNotNull();
        assertThat(list.getValue()).isNull();
        assertThat(list.getListItems()).isNull();
    }

    @Test
    void shouldCreateDynamicStringListWithBuilder() {
        // Given
        DynamicStringListElement element = DynamicStringListElement.builder()
            .code("code1")
            .label("label1")
            .build();
        List<DynamicStringListElement> items = List.of(element);

        // When
        DynamicStringList list = DynamicStringList.builder()
            .value(element)
            .listItems(items)
            .build();

        // Then
        assertThat(list).isNotNull();
        assertThat(list.getValue()).isEqualTo(element);
        assertThat(list.getListItems()).isEqualTo(items);
    }

    @Test
    void shouldCreateDynamicStringListWithJsonCreator() {
        // Given
        DynamicStringListElement element = new DynamicStringListElement("code1", "label1");
        List<DynamicStringListElement> items = List.of(element);

        // When
        DynamicStringList list = new DynamicStringList(element, items);

        // Then
        assertThat(list).isNotNull();
        assertThat(list.getValue()).isEqualTo(element);
        assertThat(list.getListItems()).isEqualTo(items);
    }

    @Test
    void shouldGetValueCode() {
        // Given
        DynamicStringListElement element = new DynamicStringListElement("code1", "label1");
        DynamicStringList list = DynamicStringList.builder()
            .value(element)
            .build();

        // When
        String valueCode = list.getValueCode();

        // Then
        assertThat(valueCode).isEqualTo("code1");
    }

    @Test
    void shouldReturnNullForValueCodeWhenValueIsNull() {
        // Given
        DynamicStringList list = DynamicStringList.builder().build();

        // When
        String valueCode = list.getValueCode();

        // Then
        assertThat(valueCode).isNull();
    }

    @Test
    void shouldUseDataAnnotationFunctionality() {
        // Given
        DynamicStringListElement element1 = new DynamicStringListElement("code1", "label1");
        List<DynamicStringListElement> items1 = List.of(element1);
        DynamicStringList list1 = new DynamicStringList(element1, items1);

        DynamicStringListElement element2 = new DynamicStringListElement("code1", "label1");
        List<DynamicStringListElement> items2 = List.of(element2);
        DynamicStringList list2 = new DynamicStringList(element2, items2);

        // When & Then
        assertThat(list1).isEqualTo(list2);
        assertThat(list1.hashCode()).isEqualTo(list2.hashCode());
        assertThat(list1.toString()).isEqualTo(list2.toString());
    }
}
