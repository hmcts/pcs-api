package uk.gov.hmcts.reform.pcs.ccd.task;

import com.github.kagkarlsson.scheduler.task.helper.RecurringTask;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import uk.gov.hmcts.reform.pcs.ccd.entity.DraftCaseDataEntity;
import uk.gov.hmcts.reform.pcs.ccd.service.respondpossessionclaim.DraftResponseDeletionService;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RespondToClaimDraftDeletionScheduledTaskTest {

    @Mock
    private DraftResponseDeletionService draftResponseDeletionService;

    private RespondToClaimDraftDeletionScheduledTask underTest;

    @BeforeEach
    void setUp() {
        underTest = new RespondToClaimDraftDeletionScheduledTask(
            "DAILY|02:00",
            7,
            2,
            50,
            draftResponseDeletionService
        );
    }

    @Test
    @DisplayName("Builds the recurring task")
    void shouldBuildRecurringTask() {
        // When
        RecurringTask<Void> task = underTest.respondToClaimDraftDeletionTask();

        // Then
        assertThat(task).isNotNull();
    }

    @Test
    @DisplayName("Does nothing when no expired drafts are returned")
    void shouldDoNothingWhenNoExpiredDrafts() {
        // Given
        when(draftResponseDeletionService.findExpiredDraftResponses(7, 50)).thenReturn(Collections.emptyList());

        // When
        underTest.runSweep();

        // Then
        verify(draftResponseDeletionService).findExpiredDraftResponses(7, 50);
        verify(draftResponseDeletionService, never()).deleteDraftData(any());
    }

    @Test
    @DisplayName("Deletes each expired draft returned by the service")
    void shouldDeleteExpiredDrafts() {
        // Given
        DraftCaseDataEntity first = new DraftCaseDataEntity();
        first.setCaseReference(1234L);
        DraftCaseDataEntity second = new DraftCaseDataEntity();
        second.setCaseReference(5678L);
        when(draftResponseDeletionService.findExpiredDraftResponses(7, 50))
            .thenReturn(List.of(first, second));

        // When
        underTest.runSweep();

        // Then
        verify(draftResponseDeletionService).deleteDraftData(first);
        verify(draftResponseDeletionService).deleteDraftData(second);
    }

    @Test
    @DisplayName("Continues processing when a draft deletion fails")
    void shouldContinueProcessingWhenDeleteFails() {
        // Given
        DraftCaseDataEntity first = new DraftCaseDataEntity();
        first.setCaseReference(1234L);
        DraftCaseDataEntity second = new DraftCaseDataEntity();
        second.setCaseReference(5678L);
        when(draftResponseDeletionService.findExpiredDraftResponses(7, 50))
            .thenReturn(List.of(first, second));
        doThrow(new RuntimeException("boom")).when(draftResponseDeletionService).deleteDraftData(first);

        // When / Then
        assertThatCode(underTest::runSweep).doesNotThrowAnyException();
        verify(draftResponseDeletionService).deleteDraftData(first);
        verify(draftResponseDeletionService).deleteDraftData(second);
    }
}
