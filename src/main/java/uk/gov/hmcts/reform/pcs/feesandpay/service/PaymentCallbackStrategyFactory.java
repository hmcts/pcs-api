package uk.gov.hmcts.reform.pcs.feesandpay.service;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.pcs.feesandpay.model.JourneyId;

import java.util.Map;

@Component
@AllArgsConstructor
public class PaymentCallbackStrategyFactory {

    private final MakeAClaimPaymentCallbackStrategy makeAClaimPaymentCallbackStrategy;

    public PaymentCallbackStrategy getStrategy(JourneyId journeyId) {
        return Map.of(
            JourneyId.RESUME_POSSESSION_CLAIM, makeAClaimPaymentCallbackStrategy
        ).get(journeyId);
    }

}
