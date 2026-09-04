package uk.gov.hmcts.reform.pcs.feesandpay.service;

import uk.gov.hmcts.reform.pcs.ccd.entity.feesandpay.FeePaymentEntity;
import uk.gov.hmcts.reform.pcs.feesandpay.model.PaymentStatusCallback;

public interface PaymentCallbackStrategy {

    void handle(PaymentStatusCallback paymentStatusCallback, FeePaymentEntity feePaymentEntity);

    /**
     * A strategy that records a system event manages its own transaction (the executor refuses to
     * join an existing one), so the caller must not wrap it and must leave the fee update to it.
     */
    default boolean handlesOwnTransaction(PaymentStatusCallback paymentStatusCallback) {
        return false;
    }

}
