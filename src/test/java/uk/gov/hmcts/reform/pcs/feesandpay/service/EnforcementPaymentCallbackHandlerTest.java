package uk.gov.hmcts.reform.pcs.feesandpay.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import uk.gov.hmcts.ccd.sdk.SystemEventResult;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.entity.feesandpay.FeePaymentEntity;
import uk.gov.hmcts.reform.pcs.feesandpay.model.PaymentStatus;
import uk.gov.hmcts.reform.pcs.feesandpay.model.PaymentStatusCallback;

import static org.assertj.core.api.Assertions.assertThat;

class EnforcementPaymentCallbackHandlerTest {

    private final EnforcementPaymentCallbackHandler handler = new EnforcementPaymentCallbackHandler();

    @Test
    void shouldRecordPaidEnforcementFeeWithoutChangingCaseState() {
        FeePaymentEntity feePayment = FeePaymentEntity.builder().paymentStatus(PaymentStatus.PAID).build();
        SystemEventResult result = handler.handle(new PaymentStatusCallback(), feePayment, State.CASE_ISSUED.name());

        assertThat(result).isEqualTo(SystemEventResult.withoutStateTransition(
            "enforcementPayment", "Enforcement fee paid"));
    }

    @ParameterizedTest
    @EnumSource(value = PaymentStatus.class, names = {"NOT_PAID", "PARTIALLY_PAID"})
    void shouldSkipPaidEventForIncompletePayments(PaymentStatus status) {
        FeePaymentEntity feePayment = FeePaymentEntity.builder().paymentStatus(status).build();

        assertThat(handler.handle(new PaymentStatusCallback(), feePayment, null)).isNull();
    }
}
