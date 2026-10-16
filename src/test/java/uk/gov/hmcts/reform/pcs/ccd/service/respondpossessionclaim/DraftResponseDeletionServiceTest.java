package uk.gov.hmcts.reform.pcs.ccd.service.respondpossessionclaim;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.pcs.ccd.entity.DraftCaseDataEntity;
import uk.gov.hmcts.reform.pcs.ccd.event.EventId;
import uk.gov.hmcts.reform.pcs.ccd.repository.DraftCaseDataRepository;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class DraftResponseDeletionServiceTest {

    @Mock
    private DraftCaseDataRepository draftCaseDataRepository;

    @Mock
    private DraftCaseDataEntity draftCaseDataEntity;

    private DraftResponseDeletionService underTest;

    @BeforeEach
    void setUp() {
        underTest = new DraftResponseDeletionService(draftCaseDataRepository);
    }

    @Test
    void shouldDeleteRespondPossessionClaimDraftsOlderThanDiscardDays() {
        // Given
        long discardDays = 30L;
        Instant beforeInvocation = Instant.now();

        // When
        underTest.deleteRespondPossessionClaimBatch(discardDays);

        // Then
        Instant afterInvocation = Instant.now();
        ArgumentCaptor<Instant> cutoffCaptor = ArgumentCaptor.forClass(Instant.class);
        verify(draftCaseDataRepository).deleteByEventIdAndCutoff(eq(EventId.respondPossessionClaim),
                                                                 cutoffCaptor.capture()
        );

        Instant cutoff = cutoffCaptor.getValue();
        assertThat(cutoff)
            .isAfterOrEqualTo(beforeInvocation.minus(discardDays, ChronoUnit.DAYS))
            .isBeforeOrEqualTo(afterInvocation.minus(discardDays, ChronoUnit.DAYS));
    }

    @Test
    void shouldUseCurrentTimeAsCutoffWhenDiscardDaysIsZero() {
        // Given
        Instant beforeInvocation = Instant.now();

        // When
        underTest.deleteRespondPossessionClaimBatch(0L);

        // Then
        Instant afterInvocation = Instant.now();
        ArgumentCaptor<Instant> cutoffCaptor = ArgumentCaptor.forClass(Instant.class);
        verify(draftCaseDataRepository).deleteByEventIdAndCutoff(eq(EventId.respondPossessionClaim),
            cutoffCaptor.capture()
        );

        Instant cutoff = cutoffCaptor.getValue();
        assertThat(cutoff.isBefore(beforeInvocation)).isFalse();
        assertThat(cutoff.isAfter(afterInvocation)).isFalse();
    }

}
