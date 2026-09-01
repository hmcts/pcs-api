package uk.gov.hmcts.reform.pcs.notify.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.legalrepresentative.OrganisationEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.respondpossessionclaim.CounterClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.respondpossessionclaim.DefendantResponseEntity;
import uk.gov.hmcts.reform.pcs.ccd.repository.CounterClaimRepository;
import uk.gov.hmcts.reform.pcs.ccd.repository.legalrepresentative.OrganisationRepository;
import uk.gov.hmcts.reform.pcs.idam.UserInfo;

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
    private OrganisationRepository legalRepresentativeOrganisationRepository;

    @Mock
    private UserInfo userInfo;

    private PaymentNotificationService underTest;

    @BeforeEach
    void setUp() {
        underTest = new PaymentNotificationService(
            notificationService,
            counterClaimRepository,
            legalRepresentativeOrganisationRepository
        );
    }

    @Test
    void shouldSendCounterClaimPaymentSuccessEmail() {
        UUID counterClaimId = UUID.randomUUID();
        UUID defendantId = UUID.randomUUID();
        PcsCaseEntity pcsCase = mock(PcsCaseEntity.class);

        CounterClaimEntity counterClaim = mock(CounterClaimEntity.class);
        PartyEntity defendant = mock(PartyEntity.class);
        when(defendant.getId()).thenReturn(defendantId);
        when(counterClaim.getParty()).thenReturn(defendant);
        when(counterClaim.getPcsCase()).thenReturn(pcsCase);

        DefendantResponseEntity defendantResponse = mock(DefendantResponseEntity.class);
        when(defendantResponse.getParty()).thenReturn(defendant);

        when(counterClaimRepository.findById(counterClaimId)).thenReturn(Optional.of(counterClaim));
        when(pcsCase.getDefendantResponses()).thenReturn(List.of(defendantResponse));

        String paymentReference = "PAY-1234";

        underTest.sendCounterClaimPaymentSuccessNotification(counterClaimId, paymentReference);

        verify(notificationService)
            .sendDefendantResponseCounterclaimPaymentSuccessEmailNotification(defendantResponse, paymentReference);
    }

    @Test
    void shouldSendCounterClaimPaymentSuccessEmailToLegalRep() {
        UUID counterClaimId = UUID.randomUUID();
        UUID defendantId = UUID.randomUUID();
        PcsCaseEntity pcsCase = mock(PcsCaseEntity.class);
        OrganisationEntity legalRepOrganisation = mock(OrganisationEntity.class);

        CounterClaimEntity counterClaim = mock(CounterClaimEntity.class);
        PartyEntity defendant = mock(PartyEntity.class);
        when(defendant.getId()).thenReturn(defendantId);
        when(counterClaim.getParty()).thenReturn(defendant);
        when(counterClaim.getPcsCase()).thenReturn(pcsCase);

        DefendantResponseEntity defendantResponse = mock(DefendantResponseEntity.class);
        when(defendantResponse.getParty()).thenReturn(defendant);
        when(defendantResponse.getPcsCase()).thenReturn(pcsCase);

        when(counterClaimRepository.findById(counterClaimId)).thenReturn(Optional.of(counterClaim));
        when(pcsCase.getDefendantResponses()).thenReturn(List.of(defendantResponse));
        when(legalRepresentativeOrganisationRepository
                 .findByPartyLinkedToOrganisationAndCaseAndActive(defendantId,pcsCase.getCaseReference()))
            .thenReturn(Optional.of(legalRepOrganisation));

        String paymentReference = "PAY-1234";

        underTest.sendCounterClaimPaymentSuccessNotification(counterClaimId, paymentReference);

        verify(notificationService)
            .sendDefendantResponseCounterclaimToOrganisationPaymentSuccess(legalRepOrganisation,
                                                                                  paymentReference,
                                                                                  pcsCase,
                                                                                  defendantResponse);
    }

    @Test
    void shouldThrowExceptionWhenCounterClaimNotFound() {
        UUID counterClaimId = UUID.randomUUID();
        when(counterClaimRepository.findById(counterClaimId)).thenReturn(Optional.empty());

        String paymentReference = "PAY-1234";

        assertThatThrownBy(() -> underTest.sendCounterClaimPaymentSuccessNotification(counterClaimId, paymentReference))
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

        when(counterClaimRepository.findById(counterClaimId)).thenReturn(Optional.of(counterClaim));
        when(counterClaim.getParty()).thenReturn(defendant);
        when(counterClaim.getPcsCase()).thenReturn(pcsCase);

        String paymentReference = "PAY-1234";

        underTest.sendCounterClaimPaymentSuccessNotification(counterClaimId, paymentReference);

        verifyNoInteractions(notificationService);
    }

    @Test
    void shouldNotSendNotificationWhenNoDefendantResponseFound() {
        UUID counterClaimId = UUID.randomUUID();
        PcsCaseEntity pcsCase = mock(PcsCaseEntity.class);

        CounterClaimEntity counterClaim = mock(CounterClaimEntity.class);
        PartyEntity defendant = mock(PartyEntity.class);
        when(counterClaim.getParty()).thenReturn(defendant);
        when(counterClaim.getPcsCase()).thenReturn(pcsCase);

        when(counterClaimRepository.findById(counterClaimId)).thenReturn(Optional.of(counterClaim));
        when(pcsCase.getDefendantResponses()).thenReturn(List.of());

        String paymentReference = "PAY-1234";

        underTest.sendCounterClaimPaymentSuccessNotification(counterClaimId, paymentReference);

        verifyNoInteractions(notificationService);
    }
}
