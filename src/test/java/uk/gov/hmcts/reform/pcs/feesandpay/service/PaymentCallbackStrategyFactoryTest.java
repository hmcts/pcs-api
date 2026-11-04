package uk.gov.hmcts.reform.pcs.feesandpay.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import uk.gov.hmcts.reform.pcs.ccd.entity.feesandpay.FeePaymentEntity;
import uk.gov.hmcts.reform.pcs.feesandpay.model.PaymentCallbackHandlerType;
import uk.gov.hmcts.reform.pcs.feesandpay.model.PaymentStatusCallback;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PaymentCallbackStrategyFactoryTest {

    @ParameterizedTest
    @EnumSource(PaymentCallbackHandlerType.class)
    void shouldReturnTheStrategyRegisteredForEachType(PaymentCallbackHandlerType type) {
        List<PaymentCallbackStrategy> strategies = Arrays.stream(PaymentCallbackHandlerType.values())
            .map(StubStrategy::new)
            .map(PaymentCallbackStrategy.class::cast)
            .toList();

        PaymentCallbackStrategy result = new PaymentCallbackStrategyFactory(strategies).getStrategy(type);

        assertThat(result).isSameAs(strategies.get(type.ordinal()));
    }

    @Test
    void shouldReturnNoStrategyForATypeWithNoneRegistered() {
        PaymentCallbackStrategyFactory underTest =
            new PaymentCallbackStrategyFactory(List.of(new StubStrategy(PaymentCallbackHandlerType.CLAIM)));

        assertThat(underTest.getStrategy(PaymentCallbackHandlerType.ENFORCEMENT)).isNull();
    }

    @Test
    void shouldRefuseTwoStrategiesForTheSameType() {
        List<PaymentCallbackStrategy> strategies = List.of(
            new StubStrategy(PaymentCallbackHandlerType.ENFORCEMENT),
            new StubStrategy(PaymentCallbackHandlerType.ENFORCEMENT));

        assertThatThrownBy(() -> new PaymentCallbackStrategyFactory(strategies))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("ENFORCEMENT");
    }

    private record StubStrategy(PaymentCallbackHandlerType handlerType) implements PaymentCallbackStrategy {

        @Override
        public void handle(PaymentStatusCallback paymentStatusCallback, FeePaymentEntity feePaymentEntity) {
            // Not called by the factory.
        }
    }
}
