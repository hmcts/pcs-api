package uk.gov.hmcts.reform.pcs.feesandpay.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.annotation.Order;
import uk.gov.hmcts.reform.pcs.feesandpay.model.FeeTypes;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class EnforcementFeesClientContextTest {

    private EnforcementFeesClientContext underTest;

    @Mock
    private EnforcementFeesApi enforcementFeesApi;
    @Mock
    private Jurisdictions jurisdictions;
    @Mock
    private ServiceName serviceName;

    @BeforeEach
    void beforeEach() {
        underTest = new EnforcementFeesClientContext(enforcementFeesApi, jurisdictions, serviceName);
    }

    @ParameterizedTest
    @MethodSource("enforcementFeeTypes")
    void shouldReturnTrueWhenSupports(FeeTypes feeType) {
        // Given // When
        boolean result = underTest.supports(feeType);

        // Then
        assertThat(result).isTrue();
    }

    @ParameterizedTest
    @MethodSource("makeAClaimFeeTypes")
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
        Order orderAnnotation = EnforcementFeesClientContext.class.getAnnotation(Order.class);
        // When // Then
        assertThat(orderAnnotation).isNotNull();
        assertThat(orderAnnotation.value()).isEqualTo(1);
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
