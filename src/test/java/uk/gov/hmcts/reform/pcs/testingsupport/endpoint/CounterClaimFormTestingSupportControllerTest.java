package uk.gov.hmcts.reform.pcs.testingsupport.endpoint;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import uk.gov.hmcts.reform.pcs.ccd.domain.respondpossessionclaim.CounterClaimState;
import uk.gov.hmcts.reform.pcs.ccd.entity.respondpossessionclaim.CounterClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.repository.CounterClaimRepository;
import uk.gov.hmcts.reform.pcs.ccd.service.counterclaimform.CounterClaimFormScheduler;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.Month;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CounterClaimFormTestingSupportControllerTest {

    private static final UUID COUNTER_CLAIM_ID = UUID.randomUUID();
    private static final LocalDateTime FIXED_NOW = LocalDateTime.of(2026, Month.JUNE, 30, 12, 0);

    @Mock
    private CounterClaimRepository counterClaimRepository;

    @Mock
    private CounterClaimFormScheduler counterClaimFormScheduler;

    private final Clock utcClock = Clock.fixed(
        FIXED_NOW.toInstant(ZoneOffset.UTC), ZoneOffset.UTC);

    private CounterClaimFormTestingSupportController controller;

    @BeforeEach
    void setUp() {
        controller = new CounterClaimFormTestingSupportController(
            counterClaimRepository, counterClaimFormScheduler, utcClock);
    }

    @Test
    void flipsStatusSetsIssuedDateWhenNullAndSchedulesGeneration() {
        CounterClaimEntity counterClaim = CounterClaimEntity.builder()
            .id(COUNTER_CLAIM_ID)
            .status(CounterClaimState.PENDING_COUNTER_CLAIM_ISSUED)
            .build();
        when(counterClaimRepository.findById(COUNTER_CLAIM_ID)).thenReturn(Optional.of(counterClaim));

        ResponseEntity<UUID> response = controller.issueAndSchedule(COUNTER_CLAIM_ID);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
        assertThat(response.getBody()).isEqualTo(COUNTER_CLAIM_ID);
        assertThat(counterClaim.getStatus()).isEqualTo(CounterClaimState.COUNTER_CLAIM_ISSUED);
        assertThat(counterClaim.getClaimIssuedDate()).isEqualTo(FIXED_NOW);
        verify(counterClaimRepository).save(counterClaim);
        verify(counterClaimFormScheduler).scheduleCounterClaimFormGeneration(COUNTER_CLAIM_ID);
    }

    @Test
    void doesNotOverwriteExistingClaimIssuedDate() {
        LocalDateTime preExisting = LocalDateTime.of(2026, Month.JUNE, 1, 9, 0);
        CounterClaimEntity counterClaim = CounterClaimEntity.builder()
            .id(COUNTER_CLAIM_ID)
            .status(CounterClaimState.PENDING_COUNTER_CLAIM_ISSUED)
            .claimIssuedDate(preExisting)
            .build();
        when(counterClaimRepository.findById(COUNTER_CLAIM_ID)).thenReturn(Optional.of(counterClaim));

        controller.issueAndSchedule(COUNTER_CLAIM_ID);

        assertThat(counterClaim.getClaimIssuedDate()).isEqualTo(preExisting);
        verify(counterClaimFormScheduler).scheduleCounterClaimFormGeneration(COUNTER_CLAIM_ID);
    }

    @Test
    void throwsWhenCounterClaimNotFound() {
        when(counterClaimRepository.findById(COUNTER_CLAIM_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> controller.issueAndSchedule(COUNTER_CLAIM_ID))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining(COUNTER_CLAIM_ID.toString());

        verify(counterClaimRepository, never()).save(any(CounterClaimEntity.class));
        verify(counterClaimFormScheduler, never()).scheduleCounterClaimFormGeneration(any(UUID.class));
    }
}
