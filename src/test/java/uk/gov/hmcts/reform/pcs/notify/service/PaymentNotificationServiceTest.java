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

import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
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

        when(feePaymentRepository.findById(feePaymentId)).thenReturn(java.util.Optional.of(feePayment));
        when(feePayment.getClaim()).thenReturn(claim);
        when(claim.getClaimParties()).thenReturn(List.of(claimParty));
        when(claim.getPcsCase()).thenReturn(pcsCase);
        when(pcsCase.getCounterClaims()).thenReturn(List.of(counterClaim));
        when(pcsCase.getDefendantResponses()).thenReturn(List.of(defendantResponse));

        underTest.sendCounterClaimPaymentSuccessNotification(feePaymentId);

        verify(notificationService)
            .sendDefendantResponseCounterclaimPaymentSuccessEmailNotification(defendantResponse);
    }
}
