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

    private PaymentNotificationService underTest;

    @BeforeEach
    void setUp() {
        underTest = new PaymentNotificationService(notificationService, counterClaimRepository);
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
        when(counterClaim.getPcsCase()).thenReturn(pcsCase);

        FeePaymentEntity feePayment = mock(FeePaymentEntity.class);
        when(feePayment.getPaymentStatus()).thenReturn(PaymentStatus.PAID);
        PartyEntity feePaymentParty = mock(PartyEntity.class);
        when(feePaymentParty.getId()).thenReturn(defendantId);
        when(feePayment.getParty()).thenReturn(feePaymentParty);

        ClaimEntity claim = mock(ClaimEntity.class);
        when(claim.getFeePayment()).thenReturn(feePayment);
        when(feePayment.getClaim()).thenReturn(claim);
        when(claim.getPcsCase()).thenReturn(pcsCase);

        DefendantResponseEntity defendantResponse = mock(DefendantResponseEntity.class);
        when(defendantResponse.getParty()).thenReturn(defendant);

        when(counterClaimRepository.findById(counterClaimId)).thenReturn(Optional.of(counterClaim));
        when(pcsCase.getClaims()).thenReturn(List.of(claim));
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
        PartyEntity defendant = mock(PartyEntity.class);

        PcsCaseEntity pcsCase = mock(PcsCaseEntity.class);

        ClaimEntity claim = mock(ClaimEntity.class);
        when(claim.getFeePayment()).thenReturn(null);

        when(counterClaimRepository.findById(counterClaimId)).thenReturn(Optional.of(counterClaim));
        when(counterClaim.getParty()).thenReturn(defendant);
        when(counterClaim.getPcsCase()).thenReturn(pcsCase);
        when(pcsCase.getClaims()).thenReturn(List.of(claim));

        underTest.sendCounterClaimPaymentSuccessNotification(counterClaimId);

        verifyNoInteractions(notificationService);
    }

    @Test
    void shouldNotSendNotificationWhenPaymentStatusIsNotPaid() {
        UUID counterClaimId = UUID.randomUUID();
        UUID defendantId = UUID.randomUUID();

        CounterClaimEntity counterClaim = mock(CounterClaimEntity.class);
        PartyEntity defendant = mock(PartyEntity.class);
        when(defendant.getId()).thenReturn(defendantId);
        when(counterClaim.getParty()).thenReturn(defendant);

        PcsCaseEntity pcsCase = mock(PcsCaseEntity.class);
        when(counterClaim.getPcsCase()).thenReturn(pcsCase);

        FeePaymentEntity feePayment = mock(FeePaymentEntity.class);
        when(feePayment.getPaymentStatus()).thenReturn(PaymentStatus.NOT_PAID);
        PartyEntity feePaymentParty = mock(PartyEntity.class);
        when(feePaymentParty.getId()).thenReturn(defendantId);
        when(feePayment.getParty()).thenReturn(feePaymentParty);

        ClaimEntity claim = mock(ClaimEntity.class);
        when(claim.getFeePayment()).thenReturn(feePayment);

        when(counterClaimRepository.findById(counterClaimId)).thenReturn(Optional.of(counterClaim));
        when(pcsCase.getClaims()).thenReturn(List.of(claim));

        underTest.sendCounterClaimPaymentSuccessNotification(counterClaimId);

        verifyNoInteractions(notificationService);
    }

    @Test
    void shouldNotSendNotificationWhenNoDefendantResponseFound() {
        UUID counterClaimId = UUID.randomUUID();
        UUID defendantId = UUID.randomUUID();

        CounterClaimEntity counterClaim = mock(CounterClaimEntity.class);
        PartyEntity defendant = mock(PartyEntity.class);
        when(defendant.getId()).thenReturn(defendantId);
        when(counterClaim.getParty()).thenReturn(defendant);

        PcsCaseEntity pcsCase = mock(PcsCaseEntity.class);
        when(counterClaim.getPcsCase()).thenReturn(pcsCase);

        FeePaymentEntity feePayment = mock(FeePaymentEntity.class);
        when(feePayment.getPaymentStatus()).thenReturn(PaymentStatus.PAID);
        PartyEntity feePaymentParty = mock(PartyEntity.class);
        when(feePaymentParty.getId()).thenReturn(defendantId);
        when(feePayment.getParty()).thenReturn(feePaymentParty);

        ClaimEntity claim = mock(ClaimEntity.class);
        when(claim.getFeePayment()).thenReturn(feePayment);
        when(feePayment.getClaim()).thenReturn(claim);
        when(claim.getPcsCase()).thenReturn(pcsCase);

        when(counterClaimRepository.findById(counterClaimId)).thenReturn(Optional.of(counterClaim));
        when(pcsCase.getClaims()).thenReturn(List.of(claim));
        when(pcsCase.getDefendantResponses()).thenReturn(List.of());

        underTest.sendCounterClaimPaymentSuccessNotification(counterClaimId);

        verifyNoInteractions(notificationService);
    }
}
