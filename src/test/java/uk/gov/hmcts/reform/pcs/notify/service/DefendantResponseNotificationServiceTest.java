package uk.gov.hmcts.reform.pcs.notify.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.respondpossessionclaim.CounterClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.respondpossessionclaim.DefendantResponseEntity;
import uk.gov.hmcts.reform.pcs.ccd.repository.DefendantResponseRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DefendantResponseNotificationServiceTest {

    @Mock
    private NotificationService notificationService;

    @Mock
    private DefendantResponseRepository defendantResponseRepository;

    private DefendantResponseNotificationService underTest;

    @BeforeEach
    void setUp() {
        underTest = new DefendantResponseNotificationService(
            notificationService,
            defendantResponseRepository
        );
    }

    @Test
    void shouldThrowExceptionWhenDefendantResponseNotFound() {
        UUID defendantResponseId = UUID.randomUUID();

        when(defendantResponseRepository.findById(defendantResponseId))
            .thenReturn(Optional.empty());

        assertThrows(
            IllegalArgumentException.class,
            () -> underTest.sendEmailNotification(defendantResponseId)
        );
    }

    @Test
    void shouldSendNoCounterclaimEmail() {
        DefendantResponseEntity response = mock(DefendantResponseEntity.class);
        PcsCaseEntity caseEntity = mock(PcsCaseEntity.class);
        PartyEntity party = mock(PartyEntity.class);

        UUID defendantResponseId = UUID.randomUUID();
        UUID partyId = UUID.randomUUID();

        when(defendantResponseRepository.findById(defendantResponseId))
            .thenReturn(Optional.of(response));

        when(response.getPcsCase()).thenReturn(caseEntity);
        when(response.getParty()).thenReturn(party);
        when(response.getId()).thenReturn(defendantResponseId);

        when(party.getId()).thenReturn(partyId);

        when(caseEntity.getCounterClaims()).thenReturn(List.of());

        underTest.sendEmailNotification(defendantResponseId);

        verify(notificationService)
            .sendDefendantResponseNoCounterclaimEmailNotification(response);

        verify(notificationService, never())
            .sendDefendantResponseCounterclaimPaymentRequiredEmailNotification(response);

        verify(notificationService, never())
            .sendDefendantResponseCounterclaimNoPaymentRequiredEmailNotification(response);
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
        when(response.getId()).thenReturn(defendantResponseId);

        when(party.getId()).thenReturn(partyId);

        when(counterClaim.getParty()).thenReturn(party);
        when(counterClaim.getHwfReferenceNumber()).thenReturn("HWF123");

        when(caseEntity.getCounterClaims()).thenReturn(List.of(counterClaim));

        underTest.sendEmailNotification(defendantResponseId);

        verify(notificationService)
            .sendDefendantResponseCounterclaimNoPaymentRequiredEmailNotification(response);

        verify(notificationService).sendClaimantDefendantHasMadeCounterclaimEmail(any());

        verify(notificationService, never())
            .sendDefendantResponseNoCounterclaimEmailNotification(response);

        verify(notificationService, never())
            .sendDefendantResponseCounterclaimPaymentRequiredEmailNotification(response);
    }

    @Test
    void shouldSendPaymentRequiredEmailWhenHwfReferenceIsNull() {
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
        when(response.getId()).thenReturn(defendantResponseId);

        when(party.getId()).thenReturn(partyId);

        when(counterClaim.getParty()).thenReturn(party);
        when(counterClaim.getHwfReferenceNumber()).thenReturn(null);

        when(caseEntity.getCounterClaims()).thenReturn(List.of(counterClaim));

        underTest.sendEmailNotification(defendantResponseId);

        verify(notificationService)
            .sendDefendantResponseCounterclaimPaymentRequiredEmailNotification(response);

        verify(notificationService, never())
            .sendDefendantResponseNoCounterclaimEmailNotification(response);

        verify(notificationService, never())
            .sendDefendantResponseCounterclaimNoPaymentRequiredEmailNotification(response);
    }

    @Test
    void shouldSendPaymentRequiredEmailWhenHwfReferenceIsBlank() {
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
        when(response.getId()).thenReturn(defendantResponseId);

        when(party.getId()).thenReturn(partyId);

        when(counterClaim.getParty()).thenReturn(party);
        when(counterClaim.getHwfReferenceNumber()).thenReturn(" ");

        when(caseEntity.getCounterClaims()).thenReturn(List.of(counterClaim));

        underTest.sendEmailNotification(defendantResponseId);

        verify(notificationService)
            .sendDefendantResponseCounterclaimPaymentRequiredEmailNotification(response);

        verify(notificationService, never())
            .sendDefendantResponseNoCounterclaimEmailNotification(response);

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

        underTest.sendEmailNotification(defendantResponseId);

        verify(notificationService)
            .sendDefendantResponseNoCounterclaimEmailNotification(response);

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
        when(matchingCounterClaim.getHwfReferenceNumber()).thenReturn("HWF123");

        when(caseEntity.getCounterClaims())
            .thenReturn(List.of(otherCounterClaim, matchingCounterClaim));

        underTest.sendEmailNotification(defendantResponseId);

        verify(notificationService)
            .sendDefendantResponseCounterclaimNoPaymentRequiredEmailNotification(response);

        verify(notificationService).sendClaimantDefendantHasMadeCounterclaimEmail(any());

        verify(notificationService, never())
            .sendDefendantResponseNoCounterclaimEmailNotification(response);

        verify(notificationService, never())
            .sendDefendantResponseCounterclaimPaymentRequiredEmailNotification(response);
    }
}
