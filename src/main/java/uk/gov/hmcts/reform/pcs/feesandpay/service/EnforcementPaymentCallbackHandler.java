package uk.gov.hmcts.reform.pcs.feesandpay.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.pcs.ccd.entity.feesandpay.FeePaymentEntity;
import uk.gov.hmcts.reform.pcs.feesandpay.model.PaymentStatusCallback;

@Component
@Slf4j
public class EnforcementPaymentCallbackHandler implements PaymentCallbackStrategy {

    @Override
    public void handle(PaymentStatusCallback paymentStatusCallback, FeePaymentEntity feePaymentEntity) {
        log.info("Enforcement payment made: {}", paymentStatusCallback);
        // To be applied within https://hmcts.atlassian.net/browse/HDPI-9662
    }
}
