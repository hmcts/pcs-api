package uk.gov.hmcts.reform.pcs.ccd.service;

import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.sdk.api.callback.AboutToStartOrSubmitResponse;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;

/**
 * Service for validating text area fields with character limits.
 */
@Service
public class TextAreaValidationService {

    // Common character limits used across the application
    public static final int STATEMENT_OF_TRUTH_CHARACTER_LIMIT = 60;
    public static final int BYTE_TEXT_LIMIT = 120;
    public static final int SHORT_TEXT_LIMIT = 250;
    public static final int MEDIUM_TEXT_LIMIT = 500;
    public static final int LONG_TEXT_LIMIT = 950;
    public static final int EXTRA_LONG_TEXT_LIMIT = 6400;
    public static final int RISK_CATEGORY_EXTRA_LONG_TEXT_LIMIT = 6800;

    // Error message template for character limit validation
    public static final String CHARACTER_LIMIT_ERROR_TEMPLATE =
        "In ‘%s’, you have entered more than the maximum number of characters (%s)";

    public void validateTextArea(String fieldValue, String fieldLabel, int maxCharacters, List<String> errors) {
        if (fieldValue != null && fieldValue.length() > maxCharacters) {
            String formattedMaxCharacters = formatNumberWithCommas(maxCharacters);
            String errorMessage = String.format(
                CHARACTER_LIMIT_ERROR_TEMPLATE,
                fieldLabel,
                formattedMaxCharacters
            );
            errors.add(errorMessage);
        }
    }

    private String formatNumberWithCommas(int number) {
        if (number >= 1000) {
            return NumberFormat.getNumberInstance(Locale.UK).format(number);
        }
        return String.valueOf(number);
    }

    public List<String> validateSingleTextArea(String fieldValue, String fieldLabel, int maxCharacters) {
        List<String> errors = new ArrayList<>();
        validateTextArea(fieldValue, fieldLabel, maxCharacters, errors);
        return errors;
    }

    public <T, S> AboutToStartOrSubmitResponse<T, S> createValidationResponse(
            T caseData, List<String> validationErrors) {
        return AboutToStartOrSubmitResponse.<T, S>builder()
            .data(caseData)
            .errors((validationErrors != null && !validationErrors.isEmpty()) ? validationErrors : null)
            .build();
    }

    public List<String> validateMultipleTextAreas(FieldValidation... fieldValidations) {
        List<String> errors = new ArrayList<>();

        for (FieldValidation validation : fieldValidations) {
            validateTextArea(validation.fieldValue, validation.fieldLabel, validation.maxCharacters, errors);
        }

        return errors;
    }

    public <T> List<String> validateSingleField(T object, Function<T, String> fieldExtractor,
                                                String fieldLabel, int maxCharacters) {
        if (object == null) {
            return new ArrayList<>();
        }

        String fieldValue = fieldExtractor.apply(object);
        return validateSingleTextArea(fieldValue, fieldLabel, maxCharacters);
    }

    /**
     * Configuration class for field validation.
     * Makes it easy to define multiple field validations.
     */
    public static class FieldValidation {
        public final String fieldValue;
        public final String fieldLabel;
        public final int maxCharacters;

        public FieldValidation(String fieldValue, String fieldLabel, int maxCharacters) {
            this.fieldValue = fieldValue;
            this.fieldLabel = fieldLabel;
            this.maxCharacters = maxCharacters;
        }

        public static FieldValidation of(String fieldValue, String fieldLabel, int maxCharacters) {
            return new FieldValidation(fieldValue, fieldLabel, maxCharacters);
        }
    }
}
