package uk.gov.hmcts.reform.pcs.ccd.task;

import com.github.kagkarlsson.scheduler.task.Execution;
import com.github.kagkarlsson.scheduler.task.ExecutionComplete;
import com.github.kagkarlsson.scheduler.task.ExecutionOperations;
import com.github.kagkarlsson.scheduler.task.FailureHandler;
import com.github.kagkarlsson.scheduler.task.TaskInstance;
import com.github.kagkarlsson.scheduler.task.helper.RecurringTask;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import uk.gov.hmcts.reform.pcs.ccd.service.respondpossessionclaim.DraftResponseDeletionService;
import uk.gov.hmcts.reform.pcs.service.FeatureFlag;
import uk.gov.hmcts.reform.pcs.service.FeatureToggleService;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RespondToClaimDraftDeletionScheduledTaskTest {

    private static final int DISCARD_AFTER_DAYS = 30;
    private static final int MAX_RETRIES = 3;
    private static final String TASK_NAME = "respond-to-claim-draft-deletion-task";

    @Mock
    private DraftResponseDeletionService draftResponseDeletionService;
    @Mock
    private FeatureToggleService featureToggleService;
    @Mock
    private ExecutionOperations<Void> executionOperations;

    private RespondToClaimDraftDeletionScheduledTask underTest;

    @BeforeEach
    void beforeEach() {
        underTest = newScheduledTask("DAILY|02:00");
        when(featureToggleService.isEnabled(FeatureFlag.RELEASE_1_DOT_4)).thenReturn(true);
    }

    @Test
    void shouldIgnoreDeleteDraftResponsesWhenNotEnabled() {
        // Given
        when(featureToggleService.isEnabled(FeatureFlag.RELEASE_1_DOT_4)).thenReturn(false);

        // When
        underTest.runSweep();

        // Then
        verify(draftResponseDeletionService, never()).deleteRespondPossessionClaimBatch(DISCARD_AFTER_DAYS);
    }

    @Test
    void shouldDeleteDraftResponsesOlderThanConfiguredNumberOfDays() {
        // Given / When
        underTest.runSweep();

        // Then
        verify(draftResponseDeletionService).deleteRespondPossessionClaimBatch(DISCARD_AFTER_DAYS);
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

        verify(draftResponseDeletionService).deleteRespondPossessionClaimBatch(DISCARD_AFTER_DAYS);
        assertSame(exception, thrown);
    }

    @Test
    void shouldBuildRecurringTaskWithExpectedName() {
        // When
        RecurringTask<Void> task = underTest.respondToClaimDraftDeletionTask();

        // Then
        assertThat(task).isNotNull();
        assertThat(task.getName()).isEqualTo(TASK_NAME);
    }

    @Test
    void shouldFailToBuildTaskWhenScheduleIsInvalid() {
        // Given
        RespondToClaimDraftDeletionScheduledTask misconfigured = newScheduledTask("NOT_A_SCHEDULE");

        // When / Then
        assertThatThrownBy(misconfigured::respondToClaimDraftDeletionTask)
            .isInstanceOf(RuntimeException.class);
    }

    @Test
    void shouldRunSweepWhenTaskIsExecuted() {
        // Given
        RecurringTask<Void> task = underTest.respondToClaimDraftDeletionTask();

        // When
        task.execute(taskInstance(), null);

        // Then
        verify(draftResponseDeletionService).deleteRespondPossessionClaimBatch(DISCARD_AFTER_DAYS);
    }

    @Test
    void shouldRescheduleWhenRetriesRemain() {
        // Given
        FailureHandler<Void> failureHandler = underTest.respondToClaimDraftDeletionTask().getFailureHandler();

        // When
        failureHandler.onFailure(failedExecution(0), executionOperations);

        // Then
        verify(executionOperations).reschedule(any(ExecutionComplete.class), any(Instant.class));
        verify(executionOperations, never()).stop();
    }

    @Test
    void shouldStopWhenMaxRetriesExceeded() {
        // Given
        FailureHandler<Void> failureHandler = underTest.respondToClaimDraftDeletionTask().getFailureHandler();

        // When
        failureHandler.onFailure(failedExecution(MAX_RETRIES), executionOperations);

        // Then
        verify(executionOperations).stop();
        verify(executionOperations, never()).reschedule(any(ExecutionComplete.class), any(Instant.class));
    }

    private RespondToClaimDraftDeletionScheduledTask newScheduledTask(String schedule) {
        return new RespondToClaimDraftDeletionScheduledTask(schedule, DISCARD_AFTER_DAYS, MAX_RETRIES,
                                                            Duration.ofSeconds(10), draftResponseDeletionService,
                                                            featureToggleService
        );
    }

    private static TaskInstance<Void> taskInstance() {
        return new TaskInstance<>(TASK_NAME, RecurringTask.INSTANCE);
    }

    private static ExecutionComplete failedExecution(int consecutiveFailures) {
        Instant now = Instant.now();
        Execution execution = new Execution(
            now, taskInstance(), false, null, null, null, consecutiveFailures, null, 1L);
        return ExecutionComplete.failure(execution, now.minusSeconds(1), now, new RuntimeException("Deletion failed"));
    }

}
