package uk.gov.hmcts.reform.pcs.ccd.type;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DynamicStringListElementTest {

    @Test
    void shouldCreateElementWithNoArgsConstructor() {
        // When
        DynamicStringListElement element = new DynamicStringListElement();
        
        // Then
        assertThat(element).isNotNull();
        assertThat(element.getCode()).isNull();
        assertThat(element.getLabel()).isNull();
    }
    
    @Test
    void shouldCreateElementWithBuilder() {
        // When
        DynamicStringListElement element = DynamicStringListElement.builder()
            .code("code1")
            .label("label1")
            .build();
        
        // Then
        assertThat(element).isNotNull();
        assertThat(element.getCode()).isEqualTo("code1");
        assertThat(element.getLabel()).isEqualTo("label1");
    }
    
    @Test
    void shouldCreateElementWithJsonCreator() {
        // When
        DynamicStringListElement element = new DynamicStringListElement("code1", "label1");
        
        // Then
        assertThat(element).isNotNull();
        assertThat(element.getCode()).isEqualTo("code1");
        assertThat(element.getLabel()).isEqualTo("label1");
    }
    
    @Test
    void shouldSetAndGetProperties() {
        // Given
        DynamicStringListElement element = new DynamicStringListElement();
        
        // When
        element.setCode("newCode");
        element.setLabel("newLabel");
        
        // Then
        assertThat(element.getCode()).isEqualTo("newCode");
        assertThat(element.getLabel()).isEqualTo("newLabel");
    }
    
    @Test
    void shouldUseDataAnnotationFunctionality() {
        // Given
        DynamicStringListElement element1 = new DynamicStringListElement("code1", "label1");
        DynamicStringListElement element2 = new DynamicStringListElement("code1", "label1");
        DynamicStringListElement element3 = new DynamicStringListElement("code2", "label2");
        
        // When & Then
        assertThat(element1).isEqualTo(element2);
        assertThat(element1).isNotEqualTo(element3);
        assertThat(element1.hashCode()).isEqualTo(element2.hashCode());
        assertThat(element1.hashCode()).isNotEqualTo(element3.hashCode());
        assertThat(element1.toString()).isEqualTo(element2.toString());
        assertThat(element1.toString()).isNotEqualTo(element3.toString());
    }
} 