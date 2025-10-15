package uk.gov.hmcts.reform.pcs.ccd.service;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.sdk.api.callback.AboutToStartOrSubmitResponse;

import java.util.ArrayList;
import java.util.List;

/**
 * Service for validating text area fields with character limits.
 * Provides reusable validation logic for midEvent callbacks across different pages.
 */
@Slf4j
@Service
@AllArgsConstructor
public class TextAreaValidationService {

    /**
     * Validates a text area field and adds an error message if the character limit is exceeded.
     *
     * @param fieldValue The text value to validate
     * @param fieldLabel The label of the field (for error message)
     * @param maxCharacters The maximum number of characters allowed
     * @param errors List to add validation errors to
     */
    public void validateTextArea(String fieldValue, String fieldLabel, int maxCharacters, List<String> errors) {
        if (fieldValue != null && fieldValue.length() > maxCharacters) {
            String errorMessage = String.format(
                "In '%s', you have entered more than the maximum number of %d characters",
                fieldLabel,
                maxCharacters
            );
            errors.add(errorMessage);
        }
    }

    /**
     * Validates a single text area field and returns validation errors.
     * This is the simplest method for single field validation.
     *
     * @param fieldValue The text value to validate
     * @param fieldLabel The label of the field (for error message)
     * @param maxCharacters The maximum number of characters allowed
     * @return List of validation errors
     */
    public List<String> validateSingleTextArea(String fieldValue, String fieldLabel, int maxCharacters) {
        List<String> errors = new ArrayList<>();
        validateTextArea(fieldValue, fieldLabel, maxCharacters, errors);
        return errors;
    }

    /**
     * Helper method to create the standard midEvent response with validation errors.
     * This makes the common pattern even simpler.
     *
     * @param caseData The case data
     * @param validationErrors List of validation errors
     * @param <T> The case data type
     * @param <S> The state type
     * @return AboutToStartOrSubmitResponse with errors if any, or success response
     */
    public <T, S> AboutToStartOrSubmitResponse<T, S> createValidationResponse(T caseData, List<String> validationErrors) {
        if (!validationErrors.isEmpty()) {
            return AboutToStartOrSubmitResponse.<T, S>builder()
                .data(caseData)
                .errors(validationErrors)
                .build();
        }
        
        return AboutToStartOrSubmitResponse.<T, S>builder()
            .data(caseData)
            .build();
    }

    /**
     * Validates multiple text area fields with the same character limit.
     * This is perfect for pages with many similar fields.
     *
     * @param fieldValidations Array of field validation configurations
     * @return List of validation errors
     */
    public List<String> validateMultipleTextAreas(FieldValidation... fieldValidations) {
        List<String> errors = new ArrayList<>();
        
        for (FieldValidation validation : fieldValidations) {
            validateTextArea(validation.fieldValue, validation.fieldLabel, validation.maxCharacters, errors);
        }
        
        return errors;
    }

    /**
     * Configuration class for field validation.
     * Makes it easy to define multiple field validations.
     */
    public static class FieldValidation {
        private final String fieldValue;
        private final String fieldLabel;
        private final int maxCharacters;

        public FieldValidation(String fieldValue, String fieldLabel, int maxCharacters) {
            this.fieldValue = fieldValue;
            this.fieldLabel = fieldLabel;
            this.maxCharacters = maxCharacters;
        }

        /**
         * Static factory method to create a FieldValidation instance.
         */
        public static FieldValidation of(String fieldValue, String fieldLabel, int maxCharacters) {
            return new FieldValidation(fieldValue, fieldLabel, maxCharacters);
        }
    }
}
