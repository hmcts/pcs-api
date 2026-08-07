package uk.gov.hmcts.reform.pcs.exception;

import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class RedactionContextTest {

    @Test
    void shouldCreateEmptyContext() {
        // Given // When
        RedactionContext context = RedactionContext.empty();

        // Then
        assertThat(context).isNotNull();
        assertThat(context.asDebugString()).isEmpty();
    }

    @Test
    void shouldCreateContextWithSingleValue() {
        // Given // When
        RedactionContext context = RedactionContext.of("key", "value");

        // Then
        assertThat(context).isNotNull();
        assertThat(context.getValue("key")).contains("value");
    }

    @Test
    void shouldCreateContextWithSingleNumericValue() {
        // Given // When
        RedactionContext context = RedactionContext.of("id", 12345L);

        // Then
        assertThat(context.getValue("id")).contains(12345L);
    }

    @Test
    void shouldBuildContextWithMultipleValues() {
        // Given // When
        RedactionContext context = RedactionContext.builder()
            .value("firstKey", "firstValue")
            .value("secondKey", 42)
            .build();

        // Then
        assertThat(context.getValue("firstKey")).contains("firstValue");
        assertThat(context.getValue("secondKey")).contains(42);
    }

    @Test
    void shouldReturnEmptyOptionalForMissingKey() {
        // Given
        RedactionContext context = RedactionContext.of("presentKey", "value");

        // When
        Optional<Object> result = context.getValue("missingKey");

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    void shouldReturnEmptyOptionalWhenGettingValueFromEmptyContext() {
        // Given
        RedactionContext context = RedactionContext.empty();

        // When
        Optional<Object> result = context.getValue("anyKey");

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    void shouldReturnEmptyDebugStringForEmptyContext() {
        // Given
        RedactionContext context = RedactionContext.empty();

        // When
        String debugString = context.asDebugString();

        // Then
        assertThat(debugString).isEmpty();
    }

    @Test
    void shouldReturnDebugStringForSingleValue() {
        // Given
        RedactionContext context = RedactionContext.of("key", "value");

        // When
        String debugString = context.asDebugString();

        // Then
        assertThat(debugString).isEqualTo("key=value");
    }

    @Test
    void shouldReturnDebugStringForMultipleValues() {
        // Given
        RedactionContext context = RedactionContext.builder()
            .value("firstKey", "firstValue")
            .value("secondKey", 42)
            .build();

        // When
        String debugString = context.asDebugString();

        // Then
        assertThat(debugString)
            .contains("firstKey=firstValue")
            .contains("secondKey=42")
            .contains(", ");
    }

    @Test
    void shouldIncludeNullValueInDebugString() {
        // Given
        RedactionContext context = RedactionContext.builder()
            .value("nullKey", null)
            .build();

        // When
        String debugString = context.asDebugString();

        // Then
        assertThat(debugString).isEqualTo("nullKey=null");
    }

    @Test
    void shouldReturnPresentOptionalWhenValueIsPresent() {
        // Given
        RedactionContext context = RedactionContext.of("key", "value");

        // When
        Optional<Object> result = context.getValue("key");

        // Then
        assertThat(result).isPresent();
    }

}
