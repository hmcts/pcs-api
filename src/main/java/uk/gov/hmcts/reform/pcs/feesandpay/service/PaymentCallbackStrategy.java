package uk.gov.hmcts.reform.pcs.feesandpay.service;

import uk.gov.hmcts.reform.pcs.ccd.entity.feesandpay.FeePaymentEntity;

public interface PaymentCallbackStrategy {

    void handle(FeePaymentEntity feePaymentEntity);

}
