package uk.gov.hmcts.reform.pcs.notify.listener;

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
import uk.gov.hmcts.reform.pcs.feesandpay.model.PaymentStatus;
import uk.gov.hmcts.reform.pcs.notify.service.DefendantResponseNotificationService;
import uk.gov.hmcts.reform.pcs.notify.service.PaymentNotificationService;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CounterClaimEntityListenerTest {

    @Mock
    private DefendantResponseNotificationService defendantResponseNotificationService;

    @Mock
    private PaymentNotificationService paymentNotificationService;

    @InjectMocks
    private CounterClaimEntityListener underTest;

    @Test
    void shouldSetPreviousStatusOnPostLoad() {
        CounterClaimEntity entity = new CounterClaimEntity();
        entity.setStatus(CounterClaimStatus.PENDING_CASE_ISSUED);

        underTest.onPostLoad(entity);

        assertEquals(CounterClaimStatus.PENDING_CASE_ISSUED, entity.getPreviousStatus());
    }

    @Test
    void shouldHandleNotificationOnPostPersistWhenStatusIsPendingCaseIssued() {
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
            .status(CounterClaimStatus.PENDING_CASE_ISSUED)
            .party(party)
            .pcsCase(pcsCase)
            .build();

        underTest.onPostPersist(entity);

        verify(defendantResponseNotificationService).sendEmailNotificationForCounterclaim(defendantResponseId);
    }

    @Test
    void shouldNotHandleNotificationOnPostPersistWhenStatusIsNotPendingCaseIssued() {
        CounterClaimEntity entity = new CounterClaimEntity();
        entity.setStatus(CounterClaimStatus.CASE_ISSUED);

        underTest.onPostPersist(entity);

        verify(defendantResponseNotificationService, never()).sendEmailNotificationForCounterclaim(null);
    }

    @Test
    void shouldDoNothingOnPostUpdateWhenStatusHasNotChanged() {
        CounterClaimEntity entity = new CounterClaimEntity();
        entity.setStatus(CounterClaimStatus.PENDING_CASE_ISSUED);
        entity.setPreviousStatus(CounterClaimStatus.PENDING_CASE_ISSUED);

        underTest.onPostUpdate(entity);

        verify(defendantResponseNotificationService, never()).sendEmailNotificationForCounterclaim(null);
        verify(paymentNotificationService, never()).sendCounterClaimPaymentSuccessNotification(null);
    }

    @Test
    void shouldHandleNotificationOnPostUpdateWhenStatusChangesToPendingCaseIssued() {
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
            .status(CounterClaimStatus.PENDING_CASE_ISSUED)
            .previousStatus(CounterClaimStatus.CASE_ISSUED)
            .party(party)
            .pcsCase(pcsCase)
            .build();

        underTest.onPostUpdate(entity);

        verify(defendantResponseNotificationService).sendEmailNotificationForCounterclaim(defendantResponseId);
    }

    @Test
    void shouldHandleNotificationOnPostUpdateWhenStatusChangesToCaseIssuedAndPaymentIsPaid() {
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
            .status(CounterClaimStatus.CASE_ISSUED)
            .previousStatus(CounterClaimStatus.PENDING_CASE_ISSUED)
            .party(party)
            .pcsCase(pcsCase)
            .build();

        underTest.onPostUpdate(entity);

        verify(paymentNotificationService).sendCounterClaimPaymentSuccessNotification(feePaymentId);
    }

    @Test
    void shouldNotSendNotificationOnPostUpdateWhenStatusChangesToCaseIssuedAndPaymentIsNotPaid() {
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
            .status(CounterClaimStatus.CASE_ISSUED)
            .previousStatus(CounterClaimStatus.PENDING_CASE_ISSUED)
            .party(party)
            .pcsCase(pcsCase)
            .build();

        underTest.onPostUpdate(entity);

        verify(paymentNotificationService, never()).sendCounterClaimPaymentSuccessNotification(feePaymentId);
    }

    @Test
    void shouldThrowExceptionOnPostUpdateWhenStatusChangesToCaseIssuedAndNoFeePaymentFound() {
        UUID counterClaimId = UUID.randomUUID();
        UUID partyId = UUID.randomUUID();

        PartyEntity party = mock(PartyEntity.class);
        lenient().when(party.getId()).thenReturn(partyId);

        PcsCaseEntity pcsCase = mock(PcsCaseEntity.class);
        when(pcsCase.getClaims()).thenReturn(List.of());

        CounterClaimEntity entity = CounterClaimEntity.builder()
            .id(counterClaimId)
            .status(CounterClaimStatus.CASE_ISSUED)
            .previousStatus(CounterClaimStatus.PENDING_CASE_ISSUED)
            .party(party)
            .pcsCase(pcsCase)
            .build();

        assertThrows(IllegalArgumentException.class, () -> underTest.onPostUpdate(entity));
    }
}
