package uk.gov.hmcts.reform.pcs.feesandpay.service;

import uk.gov.hmcts.reform.pcs.ccd.entity.feesandpay.FeePaymentEntity;
import uk.gov.hmcts.reform.pcs.feesandpay.model.PaymentCallbackHandlerType;
import uk.gov.hmcts.reform.pcs.feesandpay.model.PaymentStatusCallback;

/**
 * Handles the Payments callback for the fee payments of one {@link PaymentCallbackHandlerType}. Every
 * implementation that is a Spring bean is registered with {@link PaymentCallbackStrategyFactory}, so a module
 * that core code may not depend on, such as enforcement, can handle its own payments.
 */
public interface PaymentCallbackStrategy {

    PaymentCallbackHandlerType handlerType();

    void handle(PaymentStatusCallback paymentStatusCallback, FeePaymentEntity feePaymentEntity);

}
