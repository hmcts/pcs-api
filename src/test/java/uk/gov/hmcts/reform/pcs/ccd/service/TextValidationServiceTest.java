package uk.gov.hmcts.reform.pcs.ccd.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.ccd.sdk.api.callback.AboutToStartOrSubmitResponse;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.hmcts.reform.pcs.ccd.service.TextValidationService.CHARACTER_LIMIT_ERROR_TEMPLATE;
import static uk.gov.hmcts.reform.pcs.ccd.service.TextValidationService.EXTRA_LONG_TEXT_LIMIT;
import static uk.gov.hmcts.reform.pcs.ccd.service.TextValidationService.LONG_TEXT_LIMIT;
import static uk.gov.hmcts.reform.pcs.ccd.service.TextValidationService.MEDIUM_TEXT_LIMIT;
import static uk.gov.hmcts.reform.pcs.ccd.service.TextValidationService.SHORT_TEXT_LIMIT;

@ExtendWith(MockitoExtension.class)
@DisplayName("TextValidationService Tests")
class TextValidationServiceTest {

    private TextValidationService textValidationService;

    @BeforeEach
    void setUp() {
        textValidationService = new TextValidationService();
    }

    @Nested
    @DisplayName("validateTextArea Method Tests")
    class ValidateTextAreaTests {

        @Test
        @DisplayName("Should not add error when field value is null")
        void shouldNotAddErrorWhenFieldValueIsNull() {
            // Given
            List<String> errors = new ArrayList<>();

            // When
            textValidationService.validateTextArea(null, "Test Field", 100, errors);

            // Then
            assertThat(errors).isEmpty();
        }

        @Test
        @DisplayName("Should not add error when field value is empty")
        void shouldNotAddErrorWhenFieldValueIsEmpty() {
            // Given
            List<String> errors = new ArrayList<>();

            // When
            textValidationService.validateTextArea("", "Test Field", 100, errors);

            // Then
            assertThat(errors).isEmpty();
        }

        @Test
        @DisplayName("Should not add error when field value is within limit")
        void shouldNotAddErrorWhenFieldValueIsWithinLimit() {
            // Given
            List<String> errors = new ArrayList<>();
            String fieldValue = "a".repeat(100);

            // When
            textValidationService.validateTextArea(fieldValue, "Test Field", 100, errors);

            // Then
            assertThat(errors).isEmpty();
        }

        @Test
        @DisplayName("Should add error when field value exceeds limit")
        void shouldAddErrorWhenFieldValueExceedsLimit() {
            // Given
            List<String> errors = new ArrayList<>();
            String fieldValue = "a".repeat(101);
            String fieldLabel = "Test Field";
            int maxCharacters = 100;

            // When
            textValidationService.validateTextArea(fieldValue, fieldLabel, maxCharacters, errors);

            // Then
            assertThat(errors).hasSize(1);
            assertThat(errors.get(0)).isEqualTo(
                String.format(CHARACTER_LIMIT_ERROR_TEMPLATE, fieldLabel, maxCharacters)
            );
        }

        @Test
        @DisplayName("Should add multiple errors when called multiple times")
        void shouldAddMultipleErrorsWhenCalledMultipleTimes() {
            // Given
            List<String> errors = new ArrayList<>();

            // When
            textValidationService.validateTextArea("a".repeat(101), "Field 1", 100, errors);
            textValidationService.validateTextArea("b".repeat(201), "Field 2", 200, errors);

            // Then
            assertThat(errors).hasSize(2);
            assertThat(errors.get(0)).isEqualTo(
                String.format(CHARACTER_LIMIT_ERROR_TEMPLATE, "Field 1", 100)
            );
            assertThat(errors.get(1)).isEqualTo(
                String.format(CHARACTER_LIMIT_ERROR_TEMPLATE, "Field 2", 200)
            );
        }
    }

    @Nested
    @DisplayName("validateSingleTextArea Method Tests")
    class ValidateSingleTextAreaTests {

        @Test
        @DisplayName("Should return empty list when field value is null")
        void shouldReturnEmptyListWhenFieldValueIsNull() {
            // When
            List<String> errors = textValidationService.validateSingleTextArea(null, "Test Field", 100);

            // Then
            assertThat(errors).isEmpty();
        }

        @Test
        @DisplayName("Should return empty list when field value is within limit")
        void shouldReturnEmptyListWhenFieldValueIsWithinLimit() {
            // Given
            String fieldValue = "a".repeat(100);

            // When
            List<String> errors = textValidationService.validateSingleTextArea(fieldValue, "Test Field", 100);

            // Then
            assertThat(errors).isEmpty();
        }

        @Test
        @DisplayName("Should return error when field value exceeds limit")
        void shouldReturnErrorWhenFieldValueExceedsLimit() {
            // Given
            String fieldValue = "a".repeat(101);
            String fieldLabel = "Test Field";
            int maxCharacters = 100;

            // When
            List<String> errors = textValidationService.validateSingleTextArea(
                fieldValue, fieldLabel, maxCharacters);

            // Then
            assertThat(errors).hasSize(1);
            assertThat(errors.get(0)).isEqualTo(
                String.format(CHARACTER_LIMIT_ERROR_TEMPLATE, fieldLabel, maxCharacters)
            );
        }
    }

    @Nested
    @DisplayName("validateMultipleTextAreas Method Tests")
    class ValidateMultipleTextAreasTests {

        @Test
        @DisplayName("Should return empty list when all fields are valid")
        void shouldReturnEmptyListWhenAllFieldsAreValid() {
            // Given
            TextValidationService.FieldValidation validation1 = TextValidationService.FieldValidation.of(
                "valid text", "Field 1", 100
            );
            TextValidationService.FieldValidation validation2 = TextValidationService.FieldValidation.of(
                "another valid text", "Field 2", 200
            );

            // When
            List<String> errors = textValidationService.validateMultipleTextAreas(validation1, validation2);

            // Then
            assertThat(errors).isEmpty();
        }

        @Test
        @DisplayName("Should return errors for invalid fields")
        void shouldReturnErrorsForInvalidFields() {
            // Given
            TextValidationService.FieldValidation validation1 = TextValidationService.FieldValidation.of(
                "a".repeat(101), "Field 1", 100
            );
            TextValidationService.FieldValidation validation2 = TextValidationService.FieldValidation.of(
                "valid text", "Field 2", 200
            );
            TextValidationService.FieldValidation validation3 = TextValidationService.FieldValidation.of(
                "b".repeat(201), "Field 3", 200
            );

            // When
            List<String> errors = textValidationService.validateMultipleTextAreas(
                validation1, validation2, validation3);

            // Then
            assertThat(errors).hasSize(2);
            assertThat(errors.get(0)).isEqualTo(
                String.format(CHARACTER_LIMIT_ERROR_TEMPLATE, "Field 1", 100)
            );
            assertThat(errors.get(1)).isEqualTo(
                String.format(CHARACTER_LIMIT_ERROR_TEMPLATE, "Field 3", 200)
            );
        }

        @Test
        @DisplayName("Should handle empty validation array")
        void shouldHandleEmptyValidationArray() {
            // When
            List<String> errors = textValidationService.validateMultipleTextAreas();

            // Then
            assertThat(errors).isEmpty();
        }
    }

    @Nested
    @DisplayName("validateSingleField Method Tests")
    class ValidateSingleFieldTests {

        @Test
        @DisplayName("Should return empty list when object is null")
        void shouldReturnEmptyListWhenObjectIsNull() {
            // When
            List<String> errors = textValidationService.validateSingleField(
                null, obj -> obj.toString(), "Test Field", 100
            );

            // Then
            assertThat(errors).isEmpty();
        }

        @Test
        @DisplayName("Should return empty list when field value is valid")
        void shouldReturnEmptyListWhenFieldValueIsValid() {
            // Given
            String testObject = "valid text";

            // When
            List<String> errors = textValidationService.validateSingleField(
                testObject, String::toString, "Test Field", 100
            );

            // Then
            assertThat(errors).isEmpty();
        }

        @Test
        @DisplayName("Should return error when field value exceeds limit")
        void shouldReturnErrorWhenFieldValueExceedsLimit() {
            // Given
            String testObject = "a".repeat(101);

            // When
            List<String> errors = textValidationService.validateSingleField(
                testObject, String::toString, "Test Field", 100
            );

            // Then
            assertThat(errors).hasSize(1);
            assertThat(errors.get(0)).isEqualTo(
                String.format(CHARACTER_LIMIT_ERROR_TEMPLATE, "Test Field", 100)
            );
        }
    }

    @Nested
    @DisplayName("createValidationResponse Method Tests")
    class CreateValidationResponseTests {

        @Test
        @DisplayName("Should return success response when no errors")
        void shouldReturnSuccessResponseWhenNoErrors() {
            // Given
            String testData = "test data";
            List<String> errors = List.of();

            // When
            AboutToStartOrSubmitResponse<String, String> response =
                textValidationService.createValidationResponse(testData, errors);

            // Then
            assertThat(response.getData()).isEqualTo(testData);
            assertThat(response.getErrors()).isNullOrEmpty();
        }

        @Test
        @DisplayName("Should return error response when errors exist")
        void shouldReturnErrorResponseWhenErrorsExist() {
            // Given
            String testData = "test data";
            List<String> errors = List.of("Error 1", "Error 2");

            // When
            AboutToStartOrSubmitResponse<String, String> response =
                textValidationService.createValidationResponse(testData, errors);

            // Then
            assertThat(response.getData()).isEqualTo(testData);
            assertThat(response.getErrors()).containsExactly("Error 1", "Error 2");
        }

        @Test
        @DisplayName("Should handle null errors list")
        void shouldHandleNullErrorsList() {
            // Given
            String testData = "test data";

            // When
            AboutToStartOrSubmitResponse<String, String> response =
                textValidationService.createValidationResponse(testData, null);

            // Then
            assertThat(response.getData()).isEqualTo(testData);
            assertThat(response.getErrors()).isNull();
        }
    }

    @Nested
    @DisplayName("Character Limit Constants Tests")
    class CharacterLimitConstantsTests {

        @Test
        @DisplayName("Should have correct short text limit")
        void shouldHaveCorrectShortTextLimit() {
            assertThat(SHORT_TEXT_LIMIT).isEqualTo(250);
        }

        @Test
        @DisplayName("Should have correct medium text limit")
        void shouldHaveCorrectMediumTextLimit() {
            assertThat(MEDIUM_TEXT_LIMIT).isEqualTo(500);
        }

        @Test
        @DisplayName("Should have correct long text limit")
        void shouldHaveCorrectLongTextLimit() {
            assertThat(LONG_TEXT_LIMIT).isEqualTo(950);
        }

        @Test
        @DisplayName("Should have correct extra long text limit")
        void shouldHaveCorrectExtraLongTextLimit() {
            assertThat(EXTRA_LONG_TEXT_LIMIT).isEqualTo(6400);
        }

        @Test
        @DisplayName("Should have correct error message template")
        void shouldHaveCorrectErrorMessageTemplate() {
            assertThat(CHARACTER_LIMIT_ERROR_TEMPLATE).isEqualTo(
                "In ‘%s’, you have entered more than the maximum number of characters (%s)"
            );
        }
    }

    @Nested
    @DisplayName("FieldValidation Class Tests")
    class FieldValidationTests {

        @Test
        @DisplayName("Should create FieldValidation with correct values")
        void shouldCreateFieldValidationWithCorrectValues() {
            // Given
            String fieldValue = "test value";
            String fieldLabel = "Test Field";
            int maxCharacters = 100;

            // When
            TextValidationService.FieldValidation validation = TextValidationService.FieldValidation.of(
                fieldValue, fieldLabel, maxCharacters
            );

            // Then
            assertThat(validation.fieldValue).isEqualTo(fieldValue);
            assertThat(validation.fieldLabel).isEqualTo(fieldLabel);
            assertThat(validation.maxCharacters).isEqualTo(maxCharacters);
        }

        @Test
        @DisplayName("Should create FieldValidation with null field value")
        void shouldCreateFieldValidationWithNullFieldValue() {
            // When
            TextValidationService.FieldValidation validation = TextValidationService.FieldValidation.of(
                null, "Test Field", 100
            );

            // Then
            assertThat(validation.fieldValue).isNull();
            assertThat(validation.fieldLabel).isEqualTo("Test Field");
            assertThat(validation.maxCharacters).isEqualTo(100);
        }
    }

    @Nested
    @DisplayName("Integration Tests")
    class IntegrationTests {

        @Test
        @DisplayName("Should validate multiple fields with different limits using constants")
        void shouldValidateMultipleFieldsWithDifferentLimitsUsingConstants() {
            // Given
            TextValidationService.FieldValidation shortField = TextValidationService.FieldValidation.of(
                "a".repeat(251), "Short Field", SHORT_TEXT_LIMIT
            );
            TextValidationService.FieldValidation mediumField = TextValidationService.FieldValidation.of(
                "valid medium text", "Medium Field", MEDIUM_TEXT_LIMIT
            );
            TextValidationService.FieldValidation longField = TextValidationService.FieldValidation.of(
                "b".repeat(951), "Long Field", LONG_TEXT_LIMIT
            );

            // When
            List<String> errors = textValidationService.validateMultipleTextAreas(
                shortField, mediumField, longField);

            // Then
            assertThat(errors).hasSize(2);
            assertThat(errors.get(0)).isEqualTo(
                String.format(CHARACTER_LIMIT_ERROR_TEMPLATE, "Short Field", SHORT_TEXT_LIMIT)
            );
            assertThat(errors.get(1)).isEqualTo(
                String.format(CHARACTER_LIMIT_ERROR_TEMPLATE, "Long Field", LONG_TEXT_LIMIT)
            );
        }

        @Test
        @DisplayName("Should create complete validation response with errors")
        void shouldCreateCompleteValidationResponseWithErrors() {
            // Given
            String testData = "test case data";
            List<String> validationErrors = textValidationService.validateMultipleTextAreas(
                TextValidationService.FieldValidation.of("a".repeat(101), "Field 1", 100),
                TextValidationService.FieldValidation.of("b".repeat(201), "Field 2", 200)
            );

            // When
            AboutToStartOrSubmitResponse<String, String> response =
                textValidationService.createValidationResponse(testData, validationErrors);

            // Then
            assertThat(response.getData()).isEqualTo(testData);
            assertThat(response.getErrors()).hasSize(2);
            assertThat(response.getErrors().get(0)).isEqualTo(
                String.format(CHARACTER_LIMIT_ERROR_TEMPLATE, "Field 1", 100)
            );
            assertThat(response.getErrors().get(1)).isEqualTo(
                String.format(CHARACTER_LIMIT_ERROR_TEMPLATE, "Field 2", 200)
            );
        }
    }
}
