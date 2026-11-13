package uk.gov.hmcts.reform.pcs.feesandpay.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.EnforcementState;
import uk.gov.hmcts.reform.pcs.ccd.entity.enforcetheorder.EnforcementOrderEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.feesandpay.FeePaymentEntity;
import uk.gov.hmcts.reform.pcs.ccd.repository.enforcetheorder.EnforcementOrderRepository;
import uk.gov.hmcts.reform.pcs.exception.EnforcementApplicationNotFoundException;
import uk.gov.hmcts.reform.pcs.feesandpay.model.PaymentStatus;
import uk.gov.hmcts.reform.pcs.feesandpay.model.PaymentStatusCallback;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EnforcementPaymentCallbackHandlerTest {

    @Mock
    private EnforcementOrderRepository enforcementOrderRepository;
    @Mock
    private PaymentStatusCallback paymentStatusCallback;

    @InjectMocks
    private EnforcementPaymentCallbackHandler underTest;

    @Test
    void shouldMoveEnforcementApplicationToCaseProgressionWhenPaymentIsPaid() {
        UUID enforcementApplicationId = UUID.randomUUID();
        FeePaymentEntity feePaymentEntity = feePayment(enforcementApplicationId, PaymentStatus.PAID);
        EnforcementOrderEntity enforcementOrderEntity = enforcementOrder(
            enforcementApplicationId, EnforcementState.PENDING_CASE_ISSUED
        );
        when(enforcementOrderRepository.findById(enforcementApplicationId))
            .thenReturn(Optional.of(enforcementOrderEntity));

        underTest.handle(paymentStatusCallback, feePaymentEntity);

        assertThat(enforcementOrderEntity.getState()).isEqualTo(EnforcementState.CASE_PROGRESSION);
        verify(enforcementOrderRepository).save(enforcementOrderEntity);
    }

    @Test
    void shouldNotChangeStateWhenPaymentIsPaidButStateIsNotPendingCaseIssued() {
        UUID enforcementApplicationId = UUID.randomUUID();
        FeePaymentEntity feePaymentEntity = feePayment(enforcementApplicationId, PaymentStatus.PAID);
        EnforcementOrderEntity enforcementOrderEntity = enforcementOrder(
            enforcementApplicationId, EnforcementState.CASE_PROGRESSION
        );
        when(enforcementOrderRepository.findById(enforcementApplicationId))
            .thenReturn(Optional.of(enforcementOrderEntity));

        underTest.handle(paymentStatusCallback, feePaymentEntity);

        assertThat(enforcementOrderEntity.getState()).isEqualTo(EnforcementState.CASE_PROGRESSION);
        verify(enforcementOrderRepository, never()).save(enforcementOrderEntity);
    }

    @Test
    void shouldNotLookUpEnforcementApplicationWhenPaymentIsNotPaid() {
        FeePaymentEntity feePaymentEntity = feePayment(UUID.randomUUID(), PaymentStatus.NOT_PAID);

        underTest.handle(paymentStatusCallback, feePaymentEntity);

        verify(enforcementOrderRepository, never()).findById(any());
    }

    @Test
    void shouldThrowExceptionWhenEnforcementApplicationIsNotFound() {
        UUID enforcementApplicationId = UUID.randomUUID();
        FeePaymentEntity feePaymentEntity = feePayment(enforcementApplicationId, PaymentStatus.PAID);
        when(enforcementOrderRepository.findById(enforcementApplicationId)).thenReturn(Optional.empty());

        Throwable throwable = catchThrowable(() -> underTest.handle(paymentStatusCallback, feePaymentEntity));

        assertThat(throwable).isInstanceOf(EnforcementApplicationNotFoundException.class);
        verify(enforcementOrderRepository).findById(enforcementApplicationId);
    }

    private static FeePaymentEntity feePayment(UUID relatedEntityId, PaymentStatus paymentStatus) {
        return FeePaymentEntity.builder()
            .relatedEntityId(relatedEntityId)
            .paymentStatus(paymentStatus)
            .build();
    }

    private static EnforcementOrderEntity enforcementOrder(UUID id, EnforcementState state) {
        EnforcementOrderEntity entity = new EnforcementOrderEntity();
        entity.setId(id);
        entity.setState(state);
        return entity;
    }
}
