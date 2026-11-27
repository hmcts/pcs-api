package uk.gov.hmcts.reform.pcs.feesandpay.service;

import uk.gov.hmcts.ccd.sdk.SystemEventResult;
import uk.gov.hmcts.reform.pcs.ccd.entity.feesandpay.FeePaymentEntity;
import uk.gov.hmcts.reform.pcs.feesandpay.model.PaymentStatusCallback;

public interface PaymentCallbackStrategy {

    SystemEventResult handle(PaymentStatusCallback paymentStatusCallback, FeePaymentEntity feePaymentEntity,
                             String currentState);

}
