package uk.gov.hmcts.reform.pcs.feesandpay.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.pcs.feesandpay.model.PaymentCallbackHandlerType;

import java.util.HashMap;
import java.util.Map;

@Component
public class PaymentCallbackStrategyFactory {

    private final Map<PaymentCallbackHandlerType, PaymentCallbackStrategy> strategyMap;

    @Autowired
    public PaymentCallbackStrategyFactory(MakeAClaimPaymentCallbackHandler makeAClaimPaymentCallbackHandler,
                                          GenAppPaymentCallbackHandler genAppPaymentCallbackHandler) {
        strategyMap = new HashMap<>();
        strategyMap.put(PaymentCallbackHandlerType.CLAIM, makeAClaimPaymentCallbackHandler);
        strategyMap.put(PaymentCallbackHandlerType.GEN_APP_ISSUE, genAppPaymentCallbackHandler);
    }

    public PaymentCallbackStrategy getStrategy(PaymentCallbackHandlerType paymentCallbackHandlerType) {
        return strategyMap.get(paymentCallbackHandlerType);
    }

}
