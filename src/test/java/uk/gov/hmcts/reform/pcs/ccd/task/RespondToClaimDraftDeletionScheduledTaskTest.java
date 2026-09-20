package uk.gov.hmcts.reform.pcs.ccd.task;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import uk.gov.hmcts.reform.pcs.ccd.service.respondpossessionclaim.DraftResponseDeletionService;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RespondToClaimDraftDeletionScheduledTaskTest {

    private static final int DISCARD_AFTER_DAYS = 30;

    @Mock
    private DraftResponseDeletionService draftResponseDeletionService;

    private RespondToClaimDraftDeletionScheduledTask underTest;

    @BeforeEach
    void beforeEach() {
        underTest = new RespondToClaimDraftDeletionScheduledTask(
                "DAILY|02:00",
                DISCARD_AFTER_DAYS,
                3,
                Duration.ofSeconds(10),
                draftResponseDeletionService
            );
    }

    @Test
    void shouldDeleteDraftResponsesOlderThanConfiguredNumberOfDays() {
        // Given / When
        underTest.runSweep();

        // Then
        verify(draftResponseDeletionService)
            .deleteRespondPossessionClaimBatch(DISCARD_AFTER_DAYS);
    }

    @Test
    void shouldPropagateExceptionWhenDraftDeletionFails() {
        // Given
        RuntimeException exception = new RuntimeException("Deletion failed");
        doThrow(exception)
            .when(draftResponseDeletionService)
            .deleteRespondPossessionClaimBatch(DISCARD_AFTER_DAYS);

        // When
        RuntimeException thrown = assertThrows(RuntimeException.class, () -> underTest.runSweep());

        verify(draftResponseDeletionService)
            .deleteRespondPossessionClaimBatch(DISCARD_AFTER_DAYS);
        assertSame(exception, thrown);
    }

}
