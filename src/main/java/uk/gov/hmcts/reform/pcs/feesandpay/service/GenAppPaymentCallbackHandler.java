package uk.gov.hmcts.reform.pcs.feesandpay.service;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.pcs.ccd.entity.feesandpay.FeePaymentEntity;
import uk.gov.hmcts.reform.pcs.feesandpay.model.PaymentStatusCallback;

@AllArgsConstructor
@Component
@Slf4j
public class GenAppPaymentCallbackHandler implements PaymentCallbackStrategy {

    @Override
    public void handle(PaymentStatusCallback paymentStatusCallback, FeePaymentEntity feePaymentEntity) {
        log.info("Placeholder callback for gen app payment");
    }

}
