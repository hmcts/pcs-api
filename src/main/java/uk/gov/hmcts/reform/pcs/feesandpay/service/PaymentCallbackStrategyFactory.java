package uk.gov.hmcts.reform.pcs.feesandpay.service;

import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.pcs.feesandpay.model.PaymentCallbackHandlerType;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Component
public class PaymentCallbackStrategyFactory {

    private final Map<PaymentCallbackHandlerType, PaymentCallbackStrategy> strategyMap;

    public PaymentCallbackStrategyFactory(List<PaymentCallbackStrategy> strategies) {
        strategyMap = new EnumMap<>(PaymentCallbackHandlerType.class);
        strategies.forEach(strategy -> {
            PaymentCallbackStrategy existing = strategyMap.putIfAbsent(strategy.handlerType(), strategy);
            if (existing != null) {
                throw new IllegalStateException("Both %s and %s handle %s payment callbacks".formatted(
                    existing.getClass().getSimpleName(), strategy.getClass().getSimpleName(),
                    strategy.handlerType()));
            }
        });
    }

    public PaymentCallbackStrategy getStrategy(PaymentCallbackHandlerType paymentCallbackHandlerType) {
        return strategyMap.get(paymentCallbackHandlerType);
    }

}
