package uk.gov.hmcts.reform.pcs.ccd.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class IntegerValidationServiceTest {

    private IntegerValidationService integerValidationService;

    @BeforeEach
    void setUp() {
        integerValidationService = new IntegerValidationService();
    }

    @Test
    void shouldNotAddErrorWhenFieldValueIsNull() {
        // Given
        List<String> errors = new ArrayList<>();

        // When
        integerValidationService.validateFloatIsInteger(null, "Test Label", errors);

        // Then
        assertThat(errors).isEmpty();
    }

    @Test
    void shouldNotAddErrorWhenFieldValueIsWholeNUmber() {
        // Given
        List<String> errors = new ArrayList<>();

        // When
        integerValidationService.validateFloatIsInteger(1f, "Test Label", errors);

        // Then
        assertThat(errors).isEmpty();
    }

    @Test
    void shouldAddErrorWhenFieldValueIsNotWholeNUmber() {
        // Given
        List<String> errors = new ArrayList<>();

        // When
        integerValidationService.validateFloatIsInteger(1.5f, "Test Label", errors);

        // Then
        assertThat(errors).hasSize(1);
        assertThat(errors.getFirst())
            .isEqualTo("In ‘Test Label’, you have entered a value that is not a whole number.");
    }

}
