package uk.gov.hmcts.reform.pcs.notify.event;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.pcs.ccd.domain.respondpossessionclaim.CounterClaimStatus;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.feesandpay.FeePaymentEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.respondpossessionclaim.CounterClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.respondpossessionclaim.DefendantResponseEntity;
import uk.gov.hmcts.reform.pcs.ccd.repository.CounterClaimRepository;
import uk.gov.hmcts.reform.pcs.feesandpay.model.PaymentStatus;
import uk.gov.hmcts.reform.pcs.notify.service.DefendantResponseNotificationService;
import uk.gov.hmcts.reform.pcs.notify.service.PaymentNotificationService;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CounterClaimEventListenerTest {

    @Mock
    private CounterClaimRepository counterClaimRepository;
    @Mock
    private DefendantResponseNotificationService defendantResponseNotificationService;
    @Mock
    private PaymentNotificationService paymentNotificationService;

    @InjectMocks
    private CounterClaimEventListener underTest;

    @Test
    void shouldHandleNotificationWhenStatusChangesToPendingCaseIssued() {
        UUID counterClaimId = UUID.randomUUID();
        UUID partyId = UUID.randomUUID();
        UUID defendantResponseId = UUID.randomUUID();

        PartyEntity party = mock(PartyEntity.class);
        when(party.getId()).thenReturn(partyId);

        DefendantResponseEntity defendantResponse = mock(DefendantResponseEntity.class);
        when(defendantResponse.getParty()).thenReturn(party);
        when(defendantResponse.getId()).thenReturn(defendantResponseId);

        PcsCaseEntity pcsCase = mock(PcsCaseEntity.class);
        when(pcsCase.getDefendantResponses()).thenReturn(List.of(defendantResponse));

        CounterClaimEntity entity = CounterClaimEntity.builder()
            .id(counterClaimId)
            .status(CounterClaimStatus.PENDING_COUNTER_CLAIM_ISSUED)
            .party(party)
            .pcsCase(pcsCase)
            .build();

        when(counterClaimRepository.findById(counterClaimId)).thenReturn(Optional.of(entity));

        CounterClaimStatusUpdatedEvent event = new CounterClaimStatusUpdatedEvent(
                counterClaimId,
                null,
                CounterClaimStatus.PENDING_COUNTER_CLAIM_ISSUED
        );
        underTest.handle(event);

        verify(defendantResponseNotificationService).sendEmailNotificationForCounterclaim(defendantResponseId);
    }

    @Test
    void shouldHandleNotificationWhenStatusChangesToCaseIssuedAndPaymentIsPaid() {
        UUID counterClaimId = UUID.randomUUID();
        UUID partyId = UUID.randomUUID();
        UUID feePaymentId = UUID.randomUUID();

        PartyEntity party = mock(PartyEntity.class);
        when(party.getId()).thenReturn(partyId);

        FeePaymentEntity feePayment = mock(FeePaymentEntity.class);
        when(feePayment.getId()).thenReturn(feePaymentId);
        when(feePayment.getParty()).thenReturn(party);
        when(feePayment.getPaymentStatus()).thenReturn(PaymentStatus.PAID);

        ClaimEntity claim = mock(ClaimEntity.class);
        when(claim.getFeePayment()).thenReturn(feePayment);

        PcsCaseEntity pcsCase = mock(PcsCaseEntity.class);
        when(pcsCase.getClaims()).thenReturn(List.of(claim));

        CounterClaimEntity entity = CounterClaimEntity.builder()
            .id(counterClaimId)
            .status(CounterClaimStatus.COUNTER_CLAIM_ISSUED)
            .party(party)
            .pcsCase(pcsCase)
            .build();

        when(counterClaimRepository.findById(counterClaimId)).thenReturn(Optional.of(entity));

        CounterClaimStatusUpdatedEvent event =
            new CounterClaimStatusUpdatedEvent(counterClaimId,
                                               CounterClaimStatus.PENDING_COUNTER_CLAIM_ISSUED,
                                               CounterClaimStatus.COUNTER_CLAIM_ISSUED);
        underTest.handle(event);

        verify(paymentNotificationService).sendCounterClaimPaymentSuccessNotification(feePaymentId);
    }

    @Test
    void shouldNotSendNotificationWhenStatusChangesToCaseIssuedAndPaymentIsNotPaid() {
        UUID counterClaimId = UUID.randomUUID();
        UUID partyId = UUID.randomUUID();
        UUID feePaymentId = UUID.randomUUID();

        PartyEntity party = mock(PartyEntity.class);
        when(party.getId()).thenReturn(partyId);

        FeePaymentEntity feePayment = mock(FeePaymentEntity.class);
        when(feePayment.getId()).thenReturn(feePaymentId);
        when(feePayment.getParty()).thenReturn(party);
        when(feePayment.getPaymentStatus()).thenReturn(PaymentStatus.NOT_PAID);

        ClaimEntity claim = mock(ClaimEntity.class);
        when(claim.getFeePayment()).thenReturn(feePayment);

        PcsCaseEntity pcsCase = mock(PcsCaseEntity.class);
        when(pcsCase.getClaims()).thenReturn(List.of(claim));

        CounterClaimEntity entity = CounterClaimEntity.builder()
            .id(counterClaimId)
            .status(CounterClaimStatus.COUNTER_CLAIM_ISSUED)
            .party(party)
            .pcsCase(pcsCase)
            .build();

        when(counterClaimRepository.findById(counterClaimId)).thenReturn(Optional.of(entity));

        CounterClaimStatusUpdatedEvent event =
            new CounterClaimStatusUpdatedEvent(counterClaimId,
                                               CounterClaimStatus.PENDING_COUNTER_CLAIM_ISSUED,
                                               CounterClaimStatus.COUNTER_CLAIM_ISSUED);
        underTest.handle(event);

        verify(paymentNotificationService, never()).sendCounterClaimPaymentSuccessNotification(feePaymentId);
    }

    @Test
    void shouldThrowExceptionWhenCounterClaimNotFound() {
        UUID counterClaimId = UUID.randomUUID();
        when(counterClaimRepository.findById(counterClaimId)).thenReturn(Optional.empty());

        CounterClaimStatusUpdatedEvent event =
            new CounterClaimStatusUpdatedEvent(counterClaimId,
                                               null,
                                               CounterClaimStatus.PENDING_COUNTER_CLAIM_ISSUED);

        assertThrows(IllegalArgumentException.class, () -> underTest.handle(event));
    }
}
