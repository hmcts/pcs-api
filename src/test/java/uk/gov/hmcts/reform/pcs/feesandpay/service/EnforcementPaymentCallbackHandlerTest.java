package uk.gov.hmcts.reform.pcs.feesandpay.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.EnforcementOrder;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.EnforcementState;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.SelectEnforcementType;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.enforcetheorder.EnforcementOrderEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.feesandpay.FeePaymentEntity;
import uk.gov.hmcts.reform.pcs.ccd.repository.enforcetheorder.EnforcementOrderRepository;
import uk.gov.hmcts.reform.pcs.ccd.service.party.PartyService;
import uk.gov.hmcts.reform.pcs.exception.EnforcementApplicationNotFoundException;
import uk.gov.hmcts.reform.pcs.feesandpay.model.PaymentStatus;
import uk.gov.hmcts.reform.pcs.feesandpay.model.PaymentStatusCallback;
import uk.gov.hmcts.reform.pcs.notify.service.NotificationService;
import uk.gov.hmcts.reform.pcs.service.FeatureFlag;
import uk.gov.hmcts.reform.pcs.service.FeatureToggleService;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.pcs.ccd.service.enforcetheorder.EnforcementDataUtil.buildEnforcementTypes;

@ExtendWith(MockitoExtension.class)
class EnforcementPaymentCallbackHandlerTest {

    @Mock
    private EnforcementOrderRepository enforcementOrderRepository;
    @Mock
    private PaymentStatusCallback paymentStatusCallback;
    @Mock
    private NotificationService notificationService;
    @Mock
    private PartyService partyService;
    @Mock
    private FeatureToggleService featureToggleService;

    @InjectMocks
    private EnforcementPaymentCallbackHandler underTest;

    @Test
    void shouldMoveEnforcementApplicationToCaseProgressionWhenPaymentIsPaid() {
        UUID enforcementApplicationId = UUID.randomUUID();
        FeePaymentEntity feePaymentEntity = feePayment(enforcementApplicationId, PaymentStatus.PAID);
        EnforcementOrderEntity enforcementOrderEntity =
            enforcementOrder(enforcementApplicationId, EnforcementState.PENDING_CASE_ISSUED);
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
        EnforcementOrderEntity enforcementOrderEntity =
            enforcementOrder(enforcementApplicationId, EnforcementState.CASE_PROGRESSION);
        when(enforcementOrderRepository.findById(enforcementApplicationId))
            .thenReturn(Optional.of(enforcementOrderEntity));

        underTest.handle(paymentStatusCallback, feePaymentEntity);

        assertThat(enforcementOrderEntity.getState()).isEqualTo(EnforcementState.CASE_PROGRESSION);
        verify(enforcementOrderRepository, never()).save(enforcementOrderEntity);
        verifyNoInteractions(notificationService);
    }

    @Test
    void shouldNotLookUpEnforcementApplicationWhenPaymentIsNotPaid() {
        FeePaymentEntity feePaymentEntity = feePayment(UUID.randomUUID(), PaymentStatus.NOT_PAID);

        underTest.handle(paymentStatusCallback, feePaymentEntity);

        verify(enforcementOrderRepository, never()).findById(any());
        verifyNoInteractions(notificationService);
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

    @Test
    @DisplayName("When flag is on and all conditions are met then the warrant notification email is sent")
    void givenWarrantPaidAndEnforcementFlagOn_whenHandle_thenSendWarrantOfPossessionSubmittedEmail() {
        UUID enforcementApplicationId = UUID.randomUUID();
        EnforcementOrderEntity enforcementOrderEntity = enforcementOrder(
            enforcementApplicationId,EnforcementState.PENDING_CASE_ISSUED);

        PcsCaseEntity pcsCaseEntity = new PcsCaseEntity();

        enforcementOrderEntity.setPcsCase(pcsCaseEntity);
        enforcementOrderEntity.setEnforcementOrder(enforcementOrderOfType(SelectEnforcementType.WARRANT));

        when(enforcementOrderRepository.findById(enforcementApplicationId))
            .thenReturn(Optional.of(enforcementOrderEntity));

        when(featureToggleService.isEnabled(FeatureFlag.ENFORCEMENT))
            .thenReturn(true);

        PartyEntity expectedClaimant = new PartyEntity();
        when(partyService.getPrimaryClaimantPartyEntity(pcsCaseEntity))
            .thenReturn(expectedClaimant);

        FeePaymentEntity feePaymentEntity = feePayment(enforcementApplicationId, PaymentStatus.PAID);
        underTest.handle(paymentStatusCallback, feePaymentEntity);

        verify(notificationService).sendWarrantOfPossessionSubmittedEmail(expectedClaimant);
    }

    @Test
    @DisplayName("All conditions are met but no warrant notification email is sent while the enforcement flag is off")
    void givenWarrantPaidAndEnforcementFlagOff_whenHandle_thenDoNotSendEmail() {
        UUID enforcementApplicationId = UUID.randomUUID();
        EnforcementOrderEntity enforcementOrderEntity =
            enforcementOrder(enforcementApplicationId, EnforcementState.PENDING_CASE_ISSUED);
        enforcementOrderEntity.setEnforcementOrder(enforcementOrderOfType(SelectEnforcementType.WARRANT));

        when(enforcementOrderRepository.findById(enforcementApplicationId))
            .thenReturn(Optional.of(enforcementOrderEntity));

        when(featureToggleService.isEnabled(FeatureFlag.ENFORCEMENT))
            .thenReturn(false);

        FeePaymentEntity feePaymentEntity = feePayment(enforcementApplicationId, PaymentStatus.PAID);
        underTest.handle(paymentStatusCallback, feePaymentEntity);

        verifyNoInteractions(notificationService);
    }


    private static FeePaymentEntity feePayment(UUID relatedEntityId, PaymentStatus paymentStatus) {
        return FeePaymentEntity.builder()
            .relatedEntityId(relatedEntityId)
            .paymentStatus(paymentStatus)
            .build();
    }

    private static EnforcementOrder enforcementOrderOfType(SelectEnforcementType enforcementType) {
        return EnforcementOrder.builder()
            .chooseEnforcementType(buildEnforcementTypes(enforcementType))
            .build();
    }

    private static EnforcementOrderEntity enforcementOrder(UUID id, EnforcementState state) {
        EnforcementOrderEntity entity = new EnforcementOrderEntity();
        entity.setId(id);
        entity.setState(state);
        return entity;
    }
}
