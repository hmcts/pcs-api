package uk.gov.hmcts.reform.pcs.feesandpay.service;

import uk.gov.hmcts.reform.pcs.ccd.entity.feesandpay.FeePaymentEntity;
import uk.gov.hmcts.reform.pcs.feesandpay.model.PaymentStatusCallback;

public interface PaymentCallbackStrategy {

    void handle(PaymentStatusCallback paymentStatusCallback, FeePaymentEntity feePaymentEntity);

    default void afterSystemEvent(PaymentStatusCallback paymentStatusCallback,
                                  FeePaymentEntity feePaymentEntity) {
        // Most payment journeys complete entirely inside the system-event transaction.
    }

}
