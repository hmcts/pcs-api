package uk.gov.hmcts.reform.pcs.ccd.service;

import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class IntegerValidationService {

    public static final String INTEGER_VALIDATION_ERROR_TEMPLATE =
        "In ‘%s’, you have entered a value that is not a whole number.";

    public void validateFloatIsInteger(Float fieldValue, String fieldLabel, List<String> errors) {
        if (fieldValue != null && fieldValue % 1 != 0) {
            String errorMessage = String.format(
                INTEGER_VALIDATION_ERROR_TEMPLATE,
                fieldLabel
            );
            errors.add(errorMessage);
        }
    }
}
