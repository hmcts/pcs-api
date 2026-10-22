package uk.gov.hmcts.reform.pcs.enforcement.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.feesandpay.FeePaymentEntity;
import uk.gov.hmcts.reform.pcs.enforcement.domain.EnforcementStatus;
import uk.gov.hmcts.reform.pcs.enforcement.entity.EnforcementOrderEntity;
import uk.gov.hmcts.reform.pcs.enforcement.exception.EnforcementOrderNotFoundException;
import uk.gov.hmcts.reform.pcs.enforcement.repository.EnforcementOrderRepository;
import uk.gov.hmcts.reform.pcs.feesandpay.model.PaymentCallbackHandlerType;
import uk.gov.hmcts.reform.pcs.feesandpay.model.PaymentStatus;
import uk.gov.hmcts.reform.pcs.feesandpay.model.PaymentStatusCallback;
import uk.gov.hmcts.reform.pcs.feesandpay.service.PaymentCallbackException;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EnforcementPaymentCallbackHandlerTest {

    private static final long CASE_REFERENCE = 1234567890123456L;
    private static final UUID ENFORCEMENT_ID = UUID.fromString("0f0e0d0c-0b0a-0908-0706-050403020100");
    private static final PaymentStatusCallback CALLBACK = PaymentStatusCallback.builder()
        .serviceRequestReference("2026-1692266328473")
        .ccdCaseNumber(String.valueOf(CASE_REFERENCE))
        .build();

    @Mock
    private EnforcementOrderRepository enforcementOrderRepository;
    @Mock
    private EnforcementOrderService enforcementOrderService;

    @InjectMocks
    private EnforcementPaymentCallbackHandler underTest;

    @Test
    void shouldHandleEnforcementFeePaymentCallbacks() {
        assertThat(underTest.handlerType()).isEqualTo(PaymentCallbackHandlerType.ENFORCEMENT);
    }

    @Test
    void shouldMoveAPaidApplicationAwaitingPaymentToAwaitingReview() {
        EnforcementOrderEntity enforcement = enforcement(EnforcementStatus.AWAITING_PAYMENT);
        when(enforcementOrderRepository.findById(ENFORCEMENT_ID)).thenReturn(Optional.of(enforcement));

        underTest.handle(CALLBACK, feePayment(PaymentStatus.PAID));

        verify(enforcementOrderService).moveToAwaitingReview(CASE_REFERENCE, enforcement);
    }

    @ParameterizedTest
    @EnumSource(value = PaymentStatus.class, names = {"NOT_PAID", "PARTIALLY_PAID"})
    void shouldLeaveTheApplicationAwaitingPaymentWhenThePaymentFailed(PaymentStatus status) {
        underTest.handle(CALLBACK, feePayment(status));

        verifyNoInteractions(enforcementOrderRepository, enforcementOrderService);
    }

    @ParameterizedTest
    @EnumSource(value = EnforcementStatus.class, names = "AWAITING_PAYMENT", mode = EnumSource.Mode.EXCLUDE)
    void shouldIgnoreAPaidCallbackForAnApplicationNotAwaitingPayment(EnforcementStatus status) {
        when(enforcementOrderRepository.findById(ENFORCEMENT_ID)).thenReturn(Optional.of(enforcement(status)));

        underTest.handle(CALLBACK, feePayment(PaymentStatus.PAID));

        verifyNoInteractions(enforcementOrderService);
    }

    @Test
    void shouldFailAPaidCallbackForAnEnforcementThatDoesNotExist() {
        when(enforcementOrderRepository.findById(ENFORCEMENT_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> underTest.handle(CALLBACK, feePayment(PaymentStatus.PAID)))
            .isInstanceOf(EnforcementOrderNotFoundException.class);
        verifyNoInteractions(enforcementOrderService);
    }

    @Test
    void shouldFailAPaidCallbackWithNoRelatedEnforcement() {
        FeePaymentEntity feePayment = feePayment(PaymentStatus.PAID);
        feePayment.setRelatedEntityId(null);

        assertThatThrownBy(() -> underTest.handle(CALLBACK, feePayment))
            .isInstanceOf(PaymentCallbackException.class);
        verifyNoInteractions(enforcementOrderRepository, enforcementOrderService);
    }

    @Test
    void shouldLeaveTheFeePaymentWithoutAParty() {
        when(enforcementOrderRepository.findById(ENFORCEMENT_ID))
            .thenReturn(Optional.of(enforcement(EnforcementStatus.AWAITING_PAYMENT)));
        FeePaymentEntity feePayment = feePayment(PaymentStatus.PAID);

        underTest.handle(CALLBACK, feePayment);

        // A claimant party on a paid fee payment would send the claim issued email.
        assertThat(feePayment.getParty()).isNull();
    }

    private static FeePaymentEntity feePayment(PaymentStatus status) {
        return FeePaymentEntity.builder()
            .paymentCallbackHandlerType(PaymentCallbackHandlerType.ENFORCEMENT)
            .paymentStatus(status)
            .relatedEntityId(ENFORCEMENT_ID)
            .build();
    }

    private static EnforcementOrderEntity enforcement(EnforcementStatus status) {
        PcsCaseEntity pcsCase = PcsCaseEntity.builder().caseReference(CASE_REFERENCE).build();
        ClaimEntity claim = new ClaimEntity();
        pcsCase.addClaim(claim);
        EnforcementOrderEntity enforcement = new EnforcementOrderEntity();
        enforcement.setId(ENFORCEMENT_ID);
        enforcement.setClaim(claim);
        enforcement.setStatus(status);
        return enforcement;
    }
}
