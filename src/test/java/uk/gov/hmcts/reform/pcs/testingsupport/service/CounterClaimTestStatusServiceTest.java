package uk.gov.hmcts.reform.pcs.testingsupport.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.pcs.ccd.entity.respondpossessionclaim.CounterClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.repository.CounterClaimRepository;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CounterClaimTestStatusServiceTest {

    @Mock
    private CounterClaimRepository counterClaimRepository;

    @InjectMocks
    private CounterClaimTestStatusService underTest;

    @Test
    void shouldUpdateStatus() {
        UUID counterClaimId = UUID.randomUUID();
        CounterClaimEntity counterClaim = new CounterClaimEntity();
        counterClaim.setId(counterClaimId);
        counterClaim.setStatus("PENDING_CASE_ISSUED");
        String newStatus = "CASE_ISSUED";

        when(counterClaimRepository.findById(counterClaimId)).thenReturn(Optional.of(counterClaim));

        underTest.updateStatus(counterClaimId, newStatus);

        assertEquals(newStatus, counterClaim.getStatus());
        verify(counterClaimRepository).save(counterClaim);
    }

    @Test
    void shouldThrowExceptionWhenCounterClaimNotFound() {
        UUID counterClaimId = UUID.randomUUID();
        when(counterClaimRepository.findById(counterClaimId)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> underTest.updateStatus(counterClaimId, "SOME_STATUS"));
    }
}
