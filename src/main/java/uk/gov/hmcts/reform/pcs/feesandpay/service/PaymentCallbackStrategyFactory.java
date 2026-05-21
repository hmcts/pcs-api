package uk.gov.hmcts.reform.pcs.feesandpay.service;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.pcs.feesandpay.model.PaymentCallbackHandlerType;

import java.util.Map;

@Component
@AllArgsConstructor
public class PaymentCallbackStrategyFactory {

    private final MakeAClaimPaymentCallbackHandler makeAClaimPaymentCallbackHandler;

    public PaymentCallbackStrategy getStrategy(PaymentCallbackHandlerType paymentCallbackHandlerType) {
        return Map.of(
            PaymentCallbackHandlerType.RESUME_POSSESSION_CLAIM, makeAClaimPaymentCallbackHandler
        ).get(paymentCallbackHandlerType);
    }

}
