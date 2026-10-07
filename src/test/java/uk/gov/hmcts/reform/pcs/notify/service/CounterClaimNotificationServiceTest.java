package uk.gov.hmcts.reform.pcs.notify.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.respondpossessionclaim.CounterClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.respondpossessionclaim.DefendantResponseEntity;
import uk.gov.hmcts.reform.pcs.ccd.repository.CounterClaimRepository;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CounterClaimNotificationServiceTest {

    @Mock
    private CounterClaimRepository counterClaimRepository;
    @Mock
    private NotificationService notificationService;

    private CounterClaimNotificationService underTest;

    @BeforeEach
    void setUp() {
        underTest = new CounterClaimNotificationService(counterClaimRepository, notificationService);
    }

    @Test
    void shouldSendClaimantNotificationWhenCounterClaimIssued() {
        UUID counterClaimId = UUID.randomUUID();
        ClaimEntity claim = mock(ClaimEntity.class);

        DefendantResponseEntity defendantResponse = mock(DefendantResponseEntity.class);
        when(defendantResponse.getClaim()).thenReturn(claim);

        CounterClaimEntity counterClaim = mock(CounterClaimEntity.class);
        when(counterClaim.findAssociatedDefendantResponse()).thenReturn(Optional.of(defendantResponse));

        when(counterClaimRepository.findById(counterClaimId)).thenReturn(Optional.of(counterClaim));

        underTest.sendClaimantEmailNotificationCounterClaimIssued(counterClaimId);

        verify(notificationService).sendClaimantDefendantHasMadeCounterclaimEmailNotification(claim);
    }

    @Test
    void shouldThrowExceptionWhenCounterClaimNotFoundForClaimantNotification() {
        UUID counterClaimId = UUID.randomUUID();
        when(counterClaimRepository.findById(counterClaimId)).thenReturn(Optional.empty());

        assertThrows(
            IllegalArgumentException.class,
            () -> underTest.sendClaimantEmailNotificationCounterClaimIssued(counterClaimId)
        );
    }

    @Test
    void shouldThrowExceptionWhenAssociatedDefendantResponseNotFound() {
        UUID counterClaimId = UUID.randomUUID();

        CounterClaimEntity counterClaim = mock(CounterClaimEntity.class);
        when(counterClaim.findAssociatedDefendantResponse()).thenReturn(Optional.empty());
        when(counterClaim.getId()).thenReturn(counterClaimId);

        when(counterClaimRepository.findById(counterClaimId)).thenReturn(Optional.of(counterClaim));

        assertThrows(
            IllegalArgumentException.class,
            () -> underTest.sendClaimantEmailNotificationCounterClaimIssued(counterClaimId)
        );
    }
}
