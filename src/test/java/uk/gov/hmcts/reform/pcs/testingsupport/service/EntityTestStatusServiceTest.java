package uk.gov.hmcts.reform.pcs.testingsupport.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.pcs.ccd.domain.respondpossessionclaim.CounterClaimStatus;
import uk.gov.hmcts.reform.pcs.ccd.domain.respondpossessionclaim.DefendantResponseStatus;
import uk.gov.hmcts.reform.pcs.ccd.entity.respondpossessionclaim.CounterClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.respondpossessionclaim.DefendantResponseEntity;
import uk.gov.hmcts.reform.pcs.ccd.repository.CounterClaimRepository;
import uk.gov.hmcts.reform.pcs.ccd.repository.DefendantResponseRepository;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EntityTestStatusServiceTest {

    @Mock
    private CounterClaimRepository counterClaimRepository;

    @Mock
    private DefendantResponseRepository defendantResponseRepository;

    @InjectMocks
    private EntityTestStatusService underTest;

    @Test
    void shouldUpdateCounterClaimStatus() {
        UUID counterClaimId = UUID.randomUUID();
        CounterClaimEntity counterClaim = new CounterClaimEntity();
        counterClaim.setId(counterClaimId);
        counterClaim.setStatus(CounterClaimStatus.PENDING_CASE_ISSUED);
        CounterClaimStatus newStatus = CounterClaimStatus.CASE_ISSUED;

        when(counterClaimRepository.findById(counterClaimId)).thenReturn(Optional.of(counterClaim));

        underTest.updateCounterClaimStatus(counterClaimId, newStatus);

        assertEquals(newStatus, counterClaim.getStatus());
        verify(counterClaimRepository).save(counterClaim);
    }

    @Test
    void shouldThrowExceptionWhenCounterClaimNotFound() {
        UUID counterClaimId = UUID.randomUUID();
        when(counterClaimRepository.findById(counterClaimId)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class,
                     () -> underTest.updateCounterClaimStatus(counterClaimId, CounterClaimStatus.CASE_ISSUED));
    }

    @Test
    void shouldUpdateDefendantResponseStatus() {
        UUID defendantResponseId = UUID.randomUUID();
        DefendantResponseEntity defendantResponse = new DefendantResponseEntity();
        defendantResponse.setId(defendantResponseId);
        defendantResponse.setStatus(DefendantResponseStatus.CREATED);
        DefendantResponseStatus newStatus = DefendantResponseStatus.SUBMITTED;

        when(defendantResponseRepository.findById(defendantResponseId)).thenReturn(Optional.of(defendantResponse));

        underTest.updateDefendantResponseStatus(defendantResponseId, newStatus);

        assertEquals(newStatus, defendantResponse.getStatus());
        verify(defendantResponseRepository).save(defendantResponse);
    }

    @Test
    void shouldThrowExceptionWhenDefendantResponseNotFound() {
        UUID defendantResponseId = UUID.randomUUID();
        when(defendantResponseRepository.findById(defendantResponseId)).thenReturn(Optional.empty());

        assertThrows(
            IllegalArgumentException.class,
            () -> underTest.updateDefendantResponseStatus(defendantResponseId, DefendantResponseStatus.SUBMITTED));
    }
}
