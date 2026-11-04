package uk.gov.hmcts.reform.pcs.ccd.service.enforcetheorder;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class DraftEnforcementDeletionServiceTest {

    private static final Instant NOW = Instant.parse("2026-10-21T09:00:00Z");

    @Mock
    private DraftCaseDataRepository draftCaseDataRepository;

    private DraftEnforcementDeletionService underTest;

    @BeforeEach
    void setUp() {
        underTest = new DraftEnforcementDeletionService(draftCaseDataRepository,
                                                        Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void shouldDeleteEnforcementDraftsOlderThanDiscardDays() {
        underTest.deleteEnforcementDraftsBatch(30L);

        verify(draftCaseDataRepository).deleteByEventIdAndCutoff(EventId.enforceTheOrder,
                                                                 NOW.minus(30L, ChronoUnit.DAYS));
    }

    @ParameterizedTest
    @ValueSource(longs = {0L, -1L})
    void shouldRefuseADiscardPeriodThatWouldDeleteEveryDraft(long discardDays) {
        assertThatThrownBy(() -> underTest.deleteEnforcementDraftsBatch(discardDays))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage(DraftEnforcementDeletionService.DISCARD_DAYS_TOO_SMALL.formatted(discardDays));

        verifyNoInteractions(draftCaseDataRepository);
    }

    @Test
    void shouldAcceptTheSmallestUsefulDiscardPeriod() {
        underTest.deleteEnforcementDraftsBatch(1L);

        verify(draftCaseDataRepository).deleteByEventIdAndCutoff(EventId.enforceTheOrder,
                                                                 NOW.minus(1L, ChronoUnit.DAYS));
        assertThat(NOW.minus(1L, ChronoUnit.DAYS)).isBefore(NOW);
    }
}
