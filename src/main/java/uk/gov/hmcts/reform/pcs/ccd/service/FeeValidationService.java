package uk.gov.hmcts.reform.pcs.ccd.service;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
public class FeeValidationService {
    private static final BigDecimal MIN_DEFAULT_FEE = BigDecimal.ZERO;
    private static final BigDecimal MAX_DEFAULT_FEE = BigDecimal.valueOf(1_000_000_000);

    /**
     * Validates that the fee is > 0.01 and <= 1,000,000,000.
     * Returns a list of errors; empty list means no errors.
     */
    public List<String> validateFee(BigDecimal fee, String fieldLabel) {

        List<String> errors = new ArrayList<>();

        if (fee == null || fee.compareTo(MIN_DEFAULT_FEE) <= 0 || fee.compareTo(MAX_DEFAULT_FEE) > 0) {
            errors.add(fieldLabel + " should be more than 0.01");
        }
        return errors;
    }
}
