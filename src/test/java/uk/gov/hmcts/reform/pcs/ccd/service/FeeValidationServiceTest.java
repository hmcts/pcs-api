package uk.gov.hmcts.reform.pcs.ccd.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class FeeValidationServiceTest {

    private FeeValidationService feeValidationService;

    @BeforeEach
    void setUp() {
        feeValidationService = new FeeValidationService();
    }

    @ParameterizedTest
    @MethodSource("uk.gov.hmcts.reform.pcs.ccd.page.enforcetheorder.warrant.FeeValidationTestArguments#validFees")
    void shouldReturnNoErrorsWhenFeeIsWithinValidRange(BigDecimal fee) {
        List<String> errors = feeValidationService.validateFee(fee, "Legal fee");

        assertThat(errors).isEmpty();
    }

    @ParameterizedTest
    @MethodSource("uk.gov.hmcts.reform.pcs.ccd.page.enforcetheorder.warrant.FeeValidationTestArguments#invalidFees")
    void shouldReturnErrorWhenFeeIsNullOrOutsideValidRange(BigDecimal fee) {
        List<String> errors = feeValidationService.validateFee(fee, "Legal fee");

        assertThat(errors)
            .containsExactly("Legal fee should be more than 0.01");
    }
}
