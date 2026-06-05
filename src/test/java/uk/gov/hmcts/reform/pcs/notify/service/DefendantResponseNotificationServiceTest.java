package uk.gov.hmcts.reform.pcs.notify.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.respondpossessionclaim.CounterClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.respondpossessionclaim.DefendantResponseEntity;
import uk.gov.hmcts.reform.pcs.ccd.repository.CounterClaimRepository;
import uk.gov.hmcts.reform.pcs.ccd.repository.DefendantResponseRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DefendantResponseNotificationServiceTest {

    @Mock
    private CounterClaimRepository counterClaimRepository;

    @Mock
    private NotificationService notificationService;

    @Mock
    private DefendantResponseRepository defendantResponseRepository;

    private DefendantResponseNotificationService underTest;

    @BeforeEach
    void setUp() {
        underTest = new DefendantResponseNotificationService(
            notificationService,
            defendantResponseRepository,
            counterClaimRepository
        );
    }

    @Test
    void shouldThrowExceptionWhenDefendantResponseNotFound() {
        UUID defendantResponseId = UUID.randomUUID();

        when(defendantResponseRepository.findById(defendantResponseId))
            .thenReturn(Optional.empty());

        assertThrows(
            IllegalArgumentException.class,
            () -> underTest.sendDefendantEmailNotificationForCounterclaim(defendantResponseId)
        );
    }

    @Test
    void shouldSendNoCounterClaimEmail() {
        DefendantResponseEntity response = mock(DefendantResponseEntity.class);
        PcsCaseEntity caseEntity = mock(PcsCaseEntity.class);
        PartyEntity party = mock(PartyEntity.class);

        UUID defendantResponseId = UUID.randomUUID();
        UUID partyId = UUID.randomUUID();

        when(defendantResponseRepository.findById(defendantResponseId))
            .thenReturn(Optional.of(response));

        when(response.getPcsCase()).thenReturn(caseEntity);
        when(response.getParty()).thenReturn(party);

        when(party.getId()).thenReturn(partyId);

        when(caseEntity.getCounterClaims()).thenReturn(List.of());

        underTest.sendEmailNotificationForNoCounterClaim(defendantResponseId);

        verify(notificationService)
            .sendDefendantResponseNoCounterclaimEmailNotification(response);

        verify(notificationService, never())
            .sendDefendantResponseCounterclaimPaymentRequiredEmailNotification(response);

        verify(notificationService, never())
            .sendDefendantResponseCounterclaimNoPaymentRequiredEmailNotification(response);
    }

    @Test
    void shouldNotSendNoCounterclaimEmailIfCounterclaimExists() {
        DefendantResponseEntity response = mock(DefendantResponseEntity.class);
        PcsCaseEntity caseEntity = mock(PcsCaseEntity.class);
        CounterClaimEntity counterClaim = mock(CounterClaimEntity.class);
        PartyEntity party = mock(PartyEntity.class);

        UUID defendantResponseId = UUID.randomUUID();
        UUID partyId = UUID.randomUUID();

        when(defendantResponseRepository.findById(defendantResponseId))
            .thenReturn(Optional.of(response));

        when(response.getPcsCase()).thenReturn(caseEntity);
        when(response.getParty()).thenReturn(party);

        when(party.getId()).thenReturn(partyId);
        when(counterClaim.getParty()).thenReturn(party);

        when(caseEntity.getCounterClaims()).thenReturn(List.of(counterClaim));

        underTest.sendEmailNotificationForNoCounterClaim(defendantResponseId);

        verify(notificationService, never())
            .sendDefendantResponseNoCounterclaimEmailNotification(response);
    }

    @Test
    void shouldSendNoPaymentRequiredEmail() {
        DefendantResponseEntity response = mock(DefendantResponseEntity.class);
        PcsCaseEntity caseEntity = mock(PcsCaseEntity.class);
        CounterClaimEntity counterClaim = mock(CounterClaimEntity.class);
        PartyEntity party = mock(PartyEntity.class);

        UUID defendantResponseId = UUID.randomUUID();
        UUID partyId = UUID.randomUUID();

        when(defendantResponseRepository.findById(defendantResponseId))
            .thenReturn(Optional.of(response));

        when(response.getPcsCase()).thenReturn(caseEntity);
        when(response.getParty()).thenReturn(party);

        when(party.getId()).thenReturn(partyId);

        when(counterClaim.getParty()).thenReturn(party);
        when(counterClaim.getNeedHelpWithFees()).thenReturn(VerticalYesNo.YES);
        when(counterClaim.getHwfReferenceNumber()).thenReturn("HWF123");

        when(caseEntity.getCounterClaims()).thenReturn(List.of(counterClaim));

        underTest.sendDefendantEmailNotificationForCounterclaim(defendantResponseId);

        verify(notificationService)
            .sendDefendantResponseCounterclaimNoPaymentRequiredEmailNotification(response);

        verify(notificationService, never())
            .sendDefendantResponseNoCounterclaimEmailNotification(response);

        verify(notificationService, never())
            .sendDefendantResponseCounterclaimPaymentRequiredEmailNotification(response);
    }

    @Test
    void shouldNotSendEmailWhenHwfRequestedAndHwfReferenceIsNull() {
        DefendantResponseEntity response = mock(DefendantResponseEntity.class);
        PcsCaseEntity caseEntity = mock(PcsCaseEntity.class);
        CounterClaimEntity counterClaim = mock(CounterClaimEntity.class);
        PartyEntity party = mock(PartyEntity.class);

        UUID defendantResponseId = UUID.randomUUID();
        UUID partyId = UUID.randomUUID();

        when(defendantResponseRepository.findById(defendantResponseId))
            .thenReturn(Optional.of(response));

        when(response.getPcsCase()).thenReturn(caseEntity);
        when(response.getParty()).thenReturn(party);

        when(party.getId()).thenReturn(partyId);

        when(counterClaim.getParty()).thenReturn(party);
        when(counterClaim.getNeedHelpWithFees()).thenReturn(VerticalYesNo.YES);
        when(counterClaim.getHwfReferenceNumber()).thenReturn(null);

        when(caseEntity.getCounterClaims()).thenReturn(List.of(counterClaim));

        underTest.sendDefendantEmailNotificationForCounterclaim(defendantResponseId);

        verify(notificationService, never())
            .sendDefendantResponseCounterclaimPaymentRequiredEmailNotification(response);

        verify(notificationService, never())
            .sendDefendantResponseNoCounterclaimEmailNotification(response);

        verify(notificationService, never())
            .sendDefendantResponseCounterclaimNoPaymentRequiredEmailNotification(response);
    }

    @Test
    void shouldNotSendEmailWhenHwfRequestedAndHwfReferenceIsBlank() {
        DefendantResponseEntity response = mock(DefendantResponseEntity.class);
        PcsCaseEntity caseEntity = mock(PcsCaseEntity.class);
        CounterClaimEntity counterClaim = mock(CounterClaimEntity.class);
        PartyEntity party = mock(PartyEntity.class);

        UUID defendantResponseId = UUID.randomUUID();
        UUID partyId = UUID.randomUUID();

        when(defendantResponseRepository.findById(defendantResponseId))
            .thenReturn(Optional.of(response));

        when(response.getPcsCase()).thenReturn(caseEntity);
        when(response.getParty()).thenReturn(party);

        when(party.getId()).thenReturn(partyId);

        when(counterClaim.getParty()).thenReturn(party);
        when(counterClaim.getNeedHelpWithFees()).thenReturn(VerticalYesNo.YES);
        when(counterClaim.getHwfReferenceNumber()).thenReturn(" ");

        when(caseEntity.getCounterClaims()).thenReturn(List.of(counterClaim));

        underTest.sendDefendantEmailNotificationForCounterclaim(defendantResponseId);

        verify(notificationService, never())
            .sendDefendantResponseCounterclaimPaymentRequiredEmailNotification(response);

        verify(notificationService, never())
            .sendDefendantResponseNoCounterclaimEmailNotification(response);

        verify(notificationService, never())
            .sendDefendantResponseCounterclaimNoPaymentRequiredEmailNotification(response);
    }

    @Test
    void shouldNotSendEmailWhenHwfNotRequestedButHwfReferenceIsPresent() {
        DefendantResponseEntity response = mock(DefendantResponseEntity.class);
        PcsCaseEntity caseEntity = mock(PcsCaseEntity.class);
        CounterClaimEntity counterClaim = mock(CounterClaimEntity.class);
        PartyEntity party = mock(PartyEntity.class);

        UUID defendantResponseId = UUID.randomUUID();
        UUID partyId = UUID.randomUUID();

        when(defendantResponseRepository.findById(defendantResponseId))
            .thenReturn(Optional.of(response));

        when(response.getPcsCase()).thenReturn(caseEntity);
        when(response.getParty()).thenReturn(party);

        when(party.getId()).thenReturn(partyId);

        when(counterClaim.getParty()).thenReturn(party);
        when(counterClaim.getNeedHelpWithFees()).thenReturn(VerticalYesNo.NO);
        when(counterClaim.getHwfReferenceNumber()).thenReturn("HWF123");

        when(caseEntity.getCounterClaims()).thenReturn(List.of(counterClaim));

        underTest.sendDefendantEmailNotificationForCounterclaim(defendantResponseId);

        verify(notificationService, never())
            .sendDefendantResponseCounterclaimPaymentRequiredEmailNotification(response);

        verify(notificationService, never())
            .sendDefendantResponseCounterclaimNoPaymentRequiredEmailNotification(response);
    }

    @Test
    void shouldSendPaymentRequiredEmailWhenHwfNotRequestedAndNoHwfReference() {
        DefendantResponseEntity response = mock(DefendantResponseEntity.class);
        PcsCaseEntity caseEntity = mock(PcsCaseEntity.class);
        CounterClaimEntity counterClaim = mock(CounterClaimEntity.class);
        PartyEntity party = mock(PartyEntity.class);

        UUID defendantResponseId = UUID.randomUUID();
        UUID partyId = UUID.randomUUID();

        when(defendantResponseRepository.findById(defendantResponseId))
            .thenReturn(Optional.of(response));

        when(response.getPcsCase()).thenReturn(caseEntity);
        when(response.getParty()).thenReturn(party);

        when(party.getId()).thenReturn(partyId);

        when(counterClaim.getParty()).thenReturn(party);
        when(counterClaim.getNeedHelpWithFees()).thenReturn(VerticalYesNo.NO);
        when(counterClaim.getHwfReferenceNumber()).thenReturn(null);

        when(caseEntity.getCounterClaims()).thenReturn(List.of(counterClaim));

        underTest.sendDefendantEmailNotificationForCounterclaim(defendantResponseId);

        verify(notificationService)
            .sendDefendantResponseCounterclaimPaymentRequiredEmailNotification(response);

        verify(notificationService, never())
            .sendDefendantResponseCounterclaimNoPaymentRequiredEmailNotification(response);
    }

    @Test
    void shouldIgnoreCounterclaimForDifferentParty() {
        DefendantResponseEntity response = mock(DefendantResponseEntity.class);
        PcsCaseEntity caseEntity = mock(PcsCaseEntity.class);
        CounterClaimEntity counterClaim = mock(CounterClaimEntity.class);

        PartyEntity defendantParty = mock(PartyEntity.class);
        PartyEntity otherParty = mock(PartyEntity.class);

        UUID defendantResponseId = UUID.randomUUID();
        UUID defendantPartyId = UUID.randomUUID();
        UUID otherPartyId = UUID.randomUUID();

        when(defendantResponseRepository.findById(defendantResponseId))
            .thenReturn(Optional.of(response));

        when(response.getPcsCase()).thenReturn(caseEntity);
        when(response.getParty()).thenReturn(defendantParty);
        when(response.getId()).thenReturn(defendantResponseId);

        when(defendantParty.getId()).thenReturn(defendantPartyId);
        when(otherParty.getId()).thenReturn(otherPartyId);

        when(counterClaim.getParty()).thenReturn(otherParty);

        when(caseEntity.getCounterClaims()).thenReturn(List.of(counterClaim));

        underTest.sendDefendantEmailNotificationForCounterclaim(defendantResponseId);

        verify(notificationService, never())
            .sendDefendantResponseCounterclaimPaymentRequiredEmailNotification(response);

        verify(notificationService, never())
            .sendDefendantResponseCounterclaimNoPaymentRequiredEmailNotification(response);
    }

    @Test
    void shouldSelectCounterclaimMatchingDefendantParty() {
        DefendantResponseEntity response = mock(DefendantResponseEntity.class);
        PcsCaseEntity caseEntity = mock(PcsCaseEntity.class);

        PartyEntity defendantParty = mock(PartyEntity.class);
        PartyEntity otherParty = mock(PartyEntity.class);

        CounterClaimEntity otherCounterClaim = mock(CounterClaimEntity.class);
        CounterClaimEntity matchingCounterClaim = mock(CounterClaimEntity.class);

        UUID defendantResponseId = UUID.randomUUID();
        UUID defendantPartyId = UUID.randomUUID();
        UUID otherPartyId = UUID.randomUUID();

        when(defendantResponseRepository.findById(defendantResponseId))
            .thenReturn(Optional.of(response));

        when(response.getPcsCase()).thenReturn(caseEntity);
        when(response.getParty()).thenReturn(defendantParty);

        when(defendantParty.getId()).thenReturn(defendantPartyId);
        when(otherParty.getId()).thenReturn(otherPartyId);

        when(otherCounterClaim.getParty()).thenReturn(otherParty);

        when(matchingCounterClaim.getParty()).thenReturn(defendantParty);
        when(matchingCounterClaim.getNeedHelpWithFees()).thenReturn(VerticalYesNo.YES);
        when(matchingCounterClaim.getHwfReferenceNumber()).thenReturn("HWF123");

        when(caseEntity.getCounterClaims())
            .thenReturn(List.of(otherCounterClaim, matchingCounterClaim));

        underTest.sendDefendantEmailNotificationForCounterclaim(defendantResponseId);

        verify(notificationService)
            .sendDefendantResponseCounterclaimNoPaymentRequiredEmailNotification(response);

        verify(notificationService, never())
            .sendDefendantResponseNoCounterclaimEmailNotification(response);

        verify(notificationService, never())
            .sendDefendantResponseCounterclaimPaymentRequiredEmailNotification(response);
    }

    @Test
    void shouldSendPendingCounterClaimIssuedNotification() {
        UUID counterClaimId = UUID.randomUUID();
        UUID partyId = UUID.randomUUID();
        UUID defendantResponseId = UUID.randomUUID();

        CounterClaimEntity counterClaim = mock(CounterClaimEntity.class);
        PcsCaseEntity pcsCase = mock(PcsCaseEntity.class);
        PartyEntity party = mock(PartyEntity.class);
        DefendantResponseEntity defendantResponse = mock(DefendantResponseEntity.class);

        when(counterClaimRepository.findById(counterClaimId)).thenReturn(Optional.of(counterClaim));
        when(counterClaim.getParty()).thenReturn(party);
        when(party.getId()).thenReturn(partyId);
        when(counterClaim.getPcsCase()).thenReturn(pcsCase);

        when(pcsCase.getDefendantResponses()).thenReturn(List.of(defendantResponse));
        when(defendantResponse.getParty()).thenReturn(party);
        when(defendantResponse.getId()).thenReturn(defendantResponseId);

        when(defendantResponseRepository.findById(defendantResponseId)).thenReturn(Optional.of(defendantResponse));
        when(defendantResponse.getPcsCase()).thenReturn(pcsCase);
        when(pcsCase.getCounterClaims()).thenReturn(List.of(counterClaim));
        when(counterClaim.getNeedHelpWithFees()).thenReturn(VerticalYesNo.YES);
        when(counterClaim.getHwfReferenceNumber()).thenReturn("HWF123");

        underTest.sendPendingCounterClaimIssuedNotification(counterClaimId);

        verify(notificationService)
            .sendDefendantResponseCounterclaimNoPaymentRequiredEmailNotification(defendantResponse);
    }

    @Test
    void shouldThrowExceptionWhenCounterClaimNotFoundForPendingNotification() {
        UUID counterClaimId = UUID.randomUUID();
        when(counterClaimRepository.findById(counterClaimId)).thenReturn(Optional.empty());

        assertThrows(
            IllegalArgumentException.class,
            () -> underTest.sendPendingCounterClaimIssuedNotification(counterClaimId)
        );
    }
}
