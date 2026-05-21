package uk.gov.hmcts.reform.pcs.notify.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.feesandpay.FeePaymentEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.ClaimPartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyRole;
import uk.gov.hmcts.reform.pcs.ccd.entity.respondpossessionclaim.CounterClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.respondpossessionclaim.DefendantResponseEntity;
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
    private FeePaymentRepository feePaymentRepository;

    private PaymentNotificationService underTest;

    @BeforeEach
    void setUp() {
        underTest = new PaymentNotificationService(notificationService, feePaymentRepository);
    }

    @Test
    void shouldSendCounterClaimPaymentSuccessEmail() {
        UUID feePaymentId = UUID.randomUUID();
        UUID defendantId = UUID.randomUUID();

        FeePaymentEntity feePayment = mock(FeePaymentEntity.class);
        when(feePayment.getPaymentStatus()).thenReturn(PaymentStatus.PAID);

        ClaimEntity claim = mock(ClaimEntity.class);
        PcsCaseEntity pcsCase = mock(PcsCaseEntity.class);

        PartyEntity defendant = mock(PartyEntity.class);
        when(defendant.getId()).thenReturn(defendantId);

        ClaimPartyEntity claimParty = mock(ClaimPartyEntity.class);
        when(claimParty.getRole()).thenReturn(PartyRole.DEFENDANT);
        when(claimParty.getParty()).thenReturn(defendant);

        CounterClaimEntity counterClaim = mock(CounterClaimEntity.class);
        when(counterClaim.getParty()).thenReturn(defendant);

        DefendantResponseEntity defendantResponse = mock(DefendantResponseEntity.class);
        when(defendantResponse.getParty()).thenReturn(defendant);

        when(feePaymentRepository.findById(feePaymentId)).thenReturn(Optional.of(feePayment));
        when(feePayment.getClaim()).thenReturn(claim);
        when(claim.getClaimParties()).thenReturn(List.of(claimParty));
        when(claim.getPcsCase()).thenReturn(pcsCase);
        when(pcsCase.getCounterClaims()).thenReturn(List.of(counterClaim));
        when(pcsCase.getDefendantResponses()).thenReturn(List.of(defendantResponse));

        underTest.sendCounterClaimPaymentSuccessNotification(feePaymentId);

        verify(notificationService)
            .sendDefendantResponseCounterclaimPaymentSuccessEmailNotification(defendantResponse);
    }

    @Test
    void shouldThrowExceptionWhenFeePaymentNotFound() {
        UUID feePaymentId = UUID.randomUUID();
        when(feePaymentRepository.findById(feePaymentId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> underTest.sendCounterClaimPaymentSuccessNotification(feePaymentId))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Fee payment not found: " + feePaymentId);

        verifyNoInteractions(notificationService);
    }

    @Test
    void shouldNotSendNotificationWhenPaymentStatusIsNotPaid() {
        UUID feePaymentId = UUID.randomUUID();
        FeePaymentEntity feePayment = mock(FeePaymentEntity.class);
        when(feePayment.getPaymentStatus()).thenReturn(PaymentStatus.NOT_PAID);
        when(feePaymentRepository.findById(feePaymentId)).thenReturn(Optional.of(feePayment));

        underTest.sendCounterClaimPaymentSuccessNotification(feePaymentId);

        verifyNoInteractions(notificationService);
    }

    @Test
    void shouldNotSendNotificationWhenNoDefendantFound() {
        UUID feePaymentId = UUID.randomUUID();
        FeePaymentEntity feePayment = mock(FeePaymentEntity.class);
        ClaimEntity claim = mock(ClaimEntity.class);

        when(feePayment.getPaymentStatus()).thenReturn(PaymentStatus.PAID);
        when(feePayment.getClaim()).thenReturn(claim);
        when(claim.getClaimParties()).thenReturn(List.of());
        when(feePaymentRepository.findById(feePaymentId)).thenReturn(Optional.of(feePayment));

        underTest.sendCounterClaimPaymentSuccessNotification(feePaymentId);

        verifyNoInteractions(notificationService);
    }

    @Test
    void shouldNotSendNotificationWhenNoCounterClaimFound() {
        UUID feePaymentId = UUID.randomUUID();

        FeePaymentEntity feePayment = mock(FeePaymentEntity.class);
        ClaimEntity claim = mock(ClaimEntity.class);
        PcsCaseEntity pcsCase = mock(PcsCaseEntity.class);
        PartyEntity defendant = mock(PartyEntity.class);
        ClaimPartyEntity claimParty = mock(ClaimPartyEntity.class);

        when(feePaymentRepository.findById(feePaymentId)).thenReturn(Optional.of(feePayment));
        when(feePayment.getPaymentStatus()).thenReturn(PaymentStatus.PAID);
        when(feePayment.getClaim()).thenReturn(claim);
        when(claim.getClaimParties()).thenReturn(List.of(claimParty));
        when(claimParty.getRole()).thenReturn(PartyRole.DEFENDANT);
        when(claimParty.getParty()).thenReturn(defendant);
        when(claim.getPcsCase()).thenReturn(pcsCase);
        when(pcsCase.getCounterClaims()).thenReturn(List.of());

        underTest.sendCounterClaimPaymentSuccessNotification(feePaymentId);

        verifyNoInteractions(notificationService);
    }

    @Test
    void shouldNotSendNotificationWhenNoDefendantResponseFound() {
        UUID feePaymentId = UUID.randomUUID();
        UUID defendantId = UUID.randomUUID();
        FeePaymentEntity feePayment = mock(FeePaymentEntity.class);
        ClaimEntity claim = mock(ClaimEntity.class);
        PcsCaseEntity pcsCase = mock(PcsCaseEntity.class);
        PartyEntity defendant = mock(PartyEntity.class);
        ClaimPartyEntity claimParty = mock(ClaimPartyEntity.class);
        CounterClaimEntity counterClaim = mock(CounterClaimEntity.class);

        when(defendant.getId()).thenReturn(defendantId);
        when(claimParty.getRole()).thenReturn(PartyRole.DEFENDANT);
        when(claimParty.getParty()).thenReturn(defendant);
        when(counterClaim.getParty()).thenReturn(defendant);

        when(feePayment.getPaymentStatus()).thenReturn(PaymentStatus.PAID);
        when(feePayment.getClaim()).thenReturn(claim);
        when(claim.getClaimParties()).thenReturn(List.of(claimParty));
        when(claim.getPcsCase()).thenReturn(pcsCase);
        when(pcsCase.getCounterClaims()).thenReturn(List.of(counterClaim));
        when(pcsCase.getDefendantResponses()).thenReturn(List.of());
        when(feePaymentRepository.findById(feePaymentId)).thenReturn(Optional.of(feePayment));

        underTest.sendCounterClaimPaymentSuccessNotification(feePaymentId);

        verifyNoInteractions(notificationService);
    }
}
