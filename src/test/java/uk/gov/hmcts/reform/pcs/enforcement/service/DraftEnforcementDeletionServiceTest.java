package uk.gov.hmcts.reform.pcs.enforcement.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.pcs.ccd.event.EventId;
import uk.gov.hmcts.reform.pcs.ccd.repository.DraftCaseDataRepository;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class DraftEnforcementDeletionServiceTest {

    @Mock
    private DraftCaseDataRepository draftCaseDataRepository;

    private static final Instant NOW = Instant.parse("2026-07-08T09:00:00Z");

    private DraftEnforcementDeletionService underTest;

    @BeforeEach
    void setUp() {
        underTest = new DraftEnforcementDeletionService(draftCaseDataRepository,
                                                        Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void shouldDeleteRespondPossessionClaimDraftsOlderThanDiscardDays() {
        // Given
        long discardDays = 30L;

        // When
        underTest.deleteEnforcementDraftsBatch(discardDays);

        // Then
        assertThat(capturedCutoff()).isEqualTo(NOW.minus(discardDays, ChronoUnit.DAYS));
    }

    @ParameterizedTest
    @ValueSource(longs = {0L, -1L})
    void shouldRejectADiscardPeriodThatWouldDeleteEveryDraft(long discardDays) {
        assertThatThrownBy(() -> underTest.deleteEnforcementDraftsBatch(discardDays))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("enforcement-draft-deletion.discard-after-days must be at least 1, but was "
                                    + discardDays);

        verifyNoInteractions(draftCaseDataRepository);
    }

    private Instant capturedCutoff() {
        ArgumentCaptor<Instant> cutoffCaptor = ArgumentCaptor.forClass(Instant.class);
        verify(draftCaseDataRepository).deleteByEventIdAndCutoff(eq(EventId.enforceTheOrder),
                cutoffCaptor.capture()
        );
        return cutoffCaptor.getValue();
    }
}