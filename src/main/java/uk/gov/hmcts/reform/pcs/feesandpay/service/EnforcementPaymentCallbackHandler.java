package uk.gov.hmcts.reform.pcs.feesandpay.service;

import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.SystemEventResult;
import uk.gov.hmcts.reform.pcs.ccd.entity.feesandpay.FeePaymentEntity;
import uk.gov.hmcts.reform.pcs.feesandpay.model.PaymentStatus;
import uk.gov.hmcts.reform.pcs.feesandpay.model.PaymentStatusCallback;

import static uk.gov.hmcts.reform.pcs.ccd.event.EventId.enforcementPayment;

@Component
public class EnforcementPaymentCallbackHandler implements PaymentCallbackStrategy {

    @Override
    public SystemEventResult handle(PaymentStatusCallback paymentStatusCallback, FeePaymentEntity feePaymentEntity,
                                    String currentState) {
        if (feePaymentEntity.getPaymentStatus() != PaymentStatus.PAID) {
            return null;
        }
        return SystemEventResult.withoutStateTransition(enforcementPayment.name(), "Enforcement fee paid");
    }
}
