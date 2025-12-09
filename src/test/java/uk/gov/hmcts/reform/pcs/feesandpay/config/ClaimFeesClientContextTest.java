package uk.gov.hmcts.reform.pcs.feesandpay.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import uk.gov.hmcts.reform.pcs.feesandpay.model.FeeTypes;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class ClaimFeesClientContextTest {

    @InjectMocks
    private ClaimFeesClientContext underTest;

    @ParameterizedTest
    @MethodSource("makeAClaimFeeTypes")
    void shouldReturnTrueWhenSupports(FeeTypes feeType) {
        // Given // When
        boolean result = underTest.supports(feeType);

        // Then
        assertThat(result).isTrue();
    }

    @ParameterizedTest
    @MethodSource("enforcementFeeTypes")
    void shouldReturnFalseWhenNotSupported(FeeTypes feeType) {
        // Given // When
        boolean result = underTest.supports(feeType);

        // Then
        assertThat(result).isFalse();
    }

    @Test
    void shouldReturnFalseWhenSupportsCalledWithNullFeeTypeCode() {
        // Given // When
        boolean result = underTest.supports(null);

        // Then
        assertThat(result).isFalse();
    }

    @Test
    void shouldMaintainOrderAnnotationValueOf1ForComponentPrioritization() {
        // Given
        Order orderAnnotation = ClaimFeesClientContext.class.getAnnotation(Order.class);
        // When // Then
        assertThat(orderAnnotation).isNotNull();
        assertThat(orderAnnotation.value()).isEqualTo(Ordered.LOWEST_PRECEDENCE);
    }

    private static Stream<FeeTypes> enforcementFeeTypes() {
        return Stream.of(
            FeeTypes.ENFORCEMENT_WARRANT_FEE,
            FeeTypes.ENFORCEMENT_WRIT_FEE
        );
    }

    private static Stream<FeeTypes> makeAClaimFeeTypes() {
        return Stream.of(
            FeeTypes.CASE_ISSUE_FEE,
            FeeTypes.HEARING_FEE
        );
    }

}
