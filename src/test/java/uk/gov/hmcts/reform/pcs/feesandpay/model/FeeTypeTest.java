package uk.gov.hmcts.reform.pcs.feesandpay.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

class FeeTypeTest {

    @ParameterizedTest
    @EnumSource(FeeType.class)
    void shouldGetFeeTypeFromCode(FeeType feeType) {
        // Given
        String code = feeType.getCode();

        // When / Then
        assertThat(FeeType.fromCode(code)).isEqualTo(feeType);
    }

    @Test
    void shouldThrowIllegalArgumentExceptionForUnknownCode() {
        // When
        Throwable throwable = catchThrowable(() -> FeeType.fromCode("some invalid code"));

        // Then
        assertThat(throwable).isInstanceOf(IllegalArgumentException.class);
    }

}
