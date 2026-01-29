package uk.gov.hmcts.reform.pcs.ccd.page.enforcetheorder.warrant;

import org.junit.jupiter.params.provider.Arguments;

import java.math.BigDecimal;
import java.util.stream.Stream;

public final class FeeValidationTestArguments {

    public static Stream<Arguments> validFees() {
        return Stream.of(
            Arguments.of(new BigDecimal("0.01")),
            Arguments.of(new BigDecimal("999999999.99")),
            Arguments.of(new BigDecimal("1000000000")),
            Arguments.of(BigDecimal.TEN)
        );
    }

    public static Stream<Arguments> invalidFees() {
        return Stream.of(
            Arguments.of((BigDecimal) null),
            Arguments.of(BigDecimal.ZERO),
            Arguments.of(new BigDecimal("-0.01")),
            Arguments.of(new BigDecimal("1000000000.01"))
        );
    }
}
