package uk.gov.hmcts.reform.pcs.notify.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.feesandpay.FeePaymentEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.respondpossessionclaim.CounterClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.respondpossessionclaim.DefendantResponseEntity;
import uk.gov.hmcts.reform.pcs.ccd.repository.CounterClaimRepository;
import uk.gov.hmcts.reform.pcs.ccd.repository.feeandpay.FeePaymentRepository;
import uk.gov.hmcts.reform.pcs.feesandpay.model.PaymentStatus;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentNotificationServiceTest {

    @Mock
    private NotificationService notificationService;

    @Mock
    private CounterClaimRepository counterClaimRepository;

    @Mock
    private FeePaymentRepository feePaymentRepository;

    private PaymentNotificationService underTest;

    @BeforeEach
    void setUp() {
        underTest = new PaymentNotificationService(
            notificationService,
            counterClaimRepository,
            feePaymentRepository
        );
    }

    @Test
    void shouldSendCounterClaimPaymentSuccessEmail() {
        UUID counterClaimId = UUID.randomUUID();
        UUID defendantId = UUID.randomUUID();

        CounterClaimEntity counterClaim = mock(CounterClaimEntity.class);
        PartyEntity defendant = mock(PartyEntity.class);
        when(defendant.getId()).thenReturn(defendantId);
        when(counterClaim.getParty()).thenReturn(defendant);

        PcsCaseEntity pcsCase = mock(PcsCaseEntity.class);

        FeePaymentEntity feePayment = mock(FeePaymentEntity.class);
        when(feePayment.getPaymentStatus()).thenReturn(PaymentStatus.PAID);

        ClaimEntity claim = mock(ClaimEntity.class);
        when(feePayment.getClaim()).thenReturn(claim);
        when(claim.getPcsCase()).thenReturn(pcsCase);

        DefendantResponseEntity defendantResponse = mock(DefendantResponseEntity.class);
        when(defendantResponse.getParty()).thenReturn(defendant);

        when(counterClaimRepository.findById(counterClaimId)).thenReturn(Optional.of(counterClaim));
        when(feePaymentRepository.findByRelatedEntityId(counterClaimId)).thenReturn(Optional.of(feePayment));
        when(pcsCase.getDefendantResponses()).thenReturn(List.of(defendantResponse));

        underTest.sendCounterClaimPaymentSuccessNotification(counterClaimId);

        verify(notificationService)
            .sendDefendantResponseCounterclaimPaymentSuccessEmailNotification(defendantResponse);
    }

    @Test
    void shouldThrowExceptionWhenCounterClaimNotFound() {
        UUID counterClaimId = UUID.randomUUID();
        when(counterClaimRepository.findById(counterClaimId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> underTest.sendCounterClaimPaymentSuccessNotification(counterClaimId))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Counter claim not found: " + counterClaimId);

        verifyNoInteractions(notificationService);
    }

    @Test
    void shouldNotSendNotificationWhenNoFeePaymentFoundForCounterclaim() {
        UUID counterClaimId = UUID.randomUUID();

        CounterClaimEntity counterClaim = mock(CounterClaimEntity.class);
        when(counterClaimRepository.findById(counterClaimId)).thenReturn(Optional.of(counterClaim));
        when(feePaymentRepository.findByRelatedEntityId(counterClaimId)).thenReturn(Optional.empty());

        underTest.sendCounterClaimPaymentSuccessNotification(counterClaimId);

        verifyNoInteractions(notificationService);
    }

    @Test
    void shouldNotSendNotificationWhenPaymentStatusIsNotPaid() {
        UUID counterClaimId = UUID.randomUUID();

        CounterClaimEntity counterClaim = mock(CounterClaimEntity.class);
        FeePaymentEntity feePayment = mock(FeePaymentEntity.class);
        when(feePayment.getPaymentStatus()).thenReturn(PaymentStatus.NOT_PAID);

        when(counterClaimRepository.findById(counterClaimId)).thenReturn(Optional.of(counterClaim));
        when(feePaymentRepository.findByRelatedEntityId(counterClaimId)).thenReturn(Optional.of(feePayment));

        underTest.sendCounterClaimPaymentSuccessNotification(counterClaimId);

        verifyNoInteractions(notificationService);
    }

    @Test
    void shouldNotSendNotificationWhenNoDefendantResponseFound() {
        UUID counterClaimId = UUID.randomUUID();

        CounterClaimEntity counterClaim = mock(CounterClaimEntity.class);
        PartyEntity defendant = mock(PartyEntity.class);
        when(counterClaim.getParty()).thenReturn(defendant);

        PcsCaseEntity pcsCase = mock(PcsCaseEntity.class);

        FeePaymentEntity feePayment = mock(FeePaymentEntity.class);
        when(feePayment.getPaymentStatus()).thenReturn(PaymentStatus.PAID);

        ClaimEntity claim = mock(ClaimEntity.class);
        when(feePayment.getClaim()).thenReturn(claim);
        when(claim.getPcsCase()).thenReturn(pcsCase);

        when(counterClaimRepository.findById(counterClaimId)).thenReturn(Optional.of(counterClaim));
        when(feePaymentRepository.findByRelatedEntityId(counterClaimId)).thenReturn(Optional.of(feePayment));
        when(pcsCase.getDefendantResponses()).thenReturn(List.of());

        underTest.sendCounterClaimPaymentSuccessNotification(counterClaimId);

        verifyNoInteractions(notificationService);
    }
}
