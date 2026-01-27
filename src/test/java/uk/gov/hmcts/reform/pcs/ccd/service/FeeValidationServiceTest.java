package uk.gov.hmcts.reform.pcs.ccd.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

public class FeeValidationServiceTest {

    private FeeValidationService feeValidationService;

    @BeforeEach
    void setUp() {
        feeValidationService = new FeeValidationService();
    }

    @ParameterizedTest
    @MethodSource("validFees")
    void shouldReturnNoErrorsWhenFeeIsWithinValidRange(BigDecimal fee) {
        List<String> errors = feeValidationService.validateFee(fee, "Legal fee");

        assertThat(errors).isEmpty();
    }

    @ParameterizedTest
    @MethodSource("invalidFees")
    void shouldReturnErrorWhenFeeIsNullOrOutsideValidRange(BigDecimal fee) {
        List<String> errors = feeValidationService.validateFee(fee, "Legal fee");

        assertThat(errors)
            .containsExactly("Legal fee should be more than 0.01");
    }

    @ParameterizedTest
    @MethodSource("validFeesWithCustomRange")
    void shouldReturnNoErrorsWhenFeeIsWithinCustomRange(BigDecimal fee,
                                                        BigDecimal min,
                                                        BigDecimal max) {

        List<String> errors = feeValidationService.validateFee(fee, min, max, "Court fee");

        assertThat(errors).isEmpty();
    }

    @ParameterizedTest
    @MethodSource("invalidFeesWithCustomRange")
    void shouldReturnErrorWhenFeeIsOutsideCustomRangeOrNull(BigDecimal fee,
                                                            BigDecimal min,
                                                            BigDecimal max) {

        List<String> errors = feeValidationService.validateFee(fee, min, max, "Court fee");

        assertThat(errors)
            .containsExactly("Court fee should be more than 0.01");
    }

    private static Stream<Arguments> validFees() {
        return Stream.of(
            Arguments.of(new BigDecimal("0.01")),
            Arguments.of(new BigDecimal("10.00")),
            Arguments.of(new BigDecimal("999999999.99")),
            Arguments.of(new BigDecimal("1000000000"))
        );
    }

    private static Stream<Arguments> invalidFees() {
        return Stream.of(
            Arguments.of((BigDecimal) null),
            Arguments.of(BigDecimal.ZERO),
            Arguments.of(new BigDecimal("-0.01")),
            Arguments.of(new BigDecimal("1000000000.01"))
        );
    }

    private static Stream<Arguments> validFeesWithCustomRange() {
        return Stream.of(
            Arguments.of(new BigDecimal("1.00"), BigDecimal.ZERO, new BigDecimal("100.00")),
            Arguments.of(new BigDecimal("50.00"), new BigDecimal("10.00"), new BigDecimal("100.00")),
            Arguments.of(new BigDecimal("100.00"), BigDecimal.ZERO, new BigDecimal("100.00")),
            Arguments.of(new BigDecimal("0.01"), BigDecimal.ZERO, new BigDecimal("1.00"))
        );
    }

    private static Stream<Arguments> invalidFeesWithCustomRange() {
        return Stream.of(
            Arguments.of(null, BigDecimal.ZERO, new BigDecimal("100.00")),
            Arguments.of(BigDecimal.ZERO, BigDecimal.ZERO, new BigDecimal("100.00")),
            Arguments.of(new BigDecimal("100.01"), BigDecimal.ZERO, new BigDecimal("100.00")),
            Arguments.of(new BigDecimal("-5.00"), new BigDecimal("0.01"), new BigDecimal("100.00"))
        );
    }
}
