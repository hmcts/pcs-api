package uk.gov.hmcts.reform.pcs.enforcement.task;

import com.github.kagkarlsson.scheduler.task.Execution;
import com.github.kagkarlsson.scheduler.task.ExecutionComplete;
import com.github.kagkarlsson.scheduler.task.ExecutionOperations;
import com.github.kagkarlsson.scheduler.task.FailureHandler;
import com.github.kagkarlsson.scheduler.task.TaskInstance;
import com.github.kagkarlsson.scheduler.task.helper.RecurringTask;
import com.github.kagkarlsson.scheduler.task.schedule.Schedules;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.boot.convert.ApplicationConversionService;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import uk.gov.hmcts.reform.pcs.enforcement.service.DraftEnforcementDeletionService;
import uk.gov.hmcts.reform.pcs.service.FeatureToggleService;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.pcs.service.FeatureFlag.ENFORCEMENT;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class EnforcementDraftDeletionScheduledTaskTest {

    private static final int DISCARD_AFTER_DAYS = 30;
    private static final int MAX_RETRIES = 3;
    private static final String TASK_NAME = "enforcement-draft-deletion-task";

    @Mock
    private DraftEnforcementDeletionService draftEnforcementDeletionService;
    @Mock
    private FeatureToggleService featureToggleService;
    @Mock
    private ExecutionOperations<Void> executionOperations;

    private EnforcementDraftDeletionScheduledTask underTest;

    @BeforeEach
    void beforeEach() {
        underTest = newScheduledTask("DAILY|02:00");
        when(featureToggleService.isEnabled(ENFORCEMENT)).thenReturn(true);
    }

    @Test
    void shouldIgnoreDeleteDraftResponsesWhenNotEnabled() {
        // Given
        when(featureToggleService.isEnabled(ENFORCEMENT)).thenReturn(false);

        // When
        underTest.runSweep();

        // Then
        verify(draftEnforcementDeletionService, never()).deleteEnforcementDraftsBatch(DISCARD_AFTER_DAYS);
    }

    @Test
    void shouldDeleteDraftResponsesOlderThanConfiguredNumberOfDays() {
        // Given / When
        underTest.runSweep();

        // Then
        verify(draftEnforcementDeletionService).deleteEnforcementDraftsBatch(DISCARD_AFTER_DAYS);
    }

    @Test
    void shouldPropagateExceptionWhenDraftDeletionFails() {
        // Given
        RuntimeException exception = new RuntimeException("Deletion failed");
        doThrow(exception)
                .when(draftEnforcementDeletionService)
                .deleteEnforcementDraftsBatch(DISCARD_AFTER_DAYS);

        // When
        RuntimeException thrown = assertThrows(RuntimeException.class, () -> underTest.runSweep());

        verify(draftEnforcementDeletionService).deleteEnforcementDraftsBatch(DISCARD_AFTER_DAYS);
        assertSame(exception, thrown);
    }

    @Test
    void shouldBuildRecurringTaskWithExpectedName() {
        // When
        RecurringTask<Void> task = underTest.enforcementDraftDeletionTask();

        // Then
        assertThat(task).isNotNull();
        assertThat(task.getName()).isEqualTo(TASK_NAME);
    }

    @Test
    void shouldFailToBuildTaskWhenScheduleIsInvalid() {
        // Given
        EnforcementDraftDeletionScheduledTask misconfigured = newScheduledTask("NOT_A_SCHEDULE");

        // When / Then
        assertThatThrownBy(misconfigured::enforcementDraftDeletionTask)
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    void shouldRunSweepWhenTaskIsExecuted() {
        // Given
        RecurringTask<Void> task = underTest.enforcementDraftDeletionTask();

        // When
        task.execute(taskInstance(), null);

        // Then
        verify(draftEnforcementDeletionService).deleteEnforcementDraftsBatch(DISCARD_AFTER_DAYS);
    }

    @Test
    void shouldRescheduleWhenRetriesRemain() {
        // Given
        FailureHandler<Void> failureHandler = underTest.enforcementDraftDeletionTask().getFailureHandler();

        // When
        failureHandler.onFailure(failedExecution(0), executionOperations);

        // Then
        verify(executionOperations).reschedule(any(ExecutionComplete.class), any(Instant.class));
        verify(executionOperations, never()).stop();
    }

    @Test
    void shouldReturnToTheRecurringScheduleWhenMaxRetriesExceeded() {
        // Given
        FailureHandler<Void> failureHandler = underTest.enforcementDraftDeletionTask().getFailureHandler();
        ExecutionComplete execution = failedExecution(MAX_RETRIES);

        // When
        failureHandler.onFailure(execution, executionOperations);

        // Then
        verify(executionOperations).reschedule(eq(execution),
                eq(Schedules.parseSchedule("DAILY|02:00").getNextExecutionTime(execution)));
        verify(executionOperations, never()).stop();
    }

    @Test
    void shouldBindABackoffDelayWithoutAUnitAsSeconds() {
        new ApplicationContextRunner()
                .withInitializer(context -> context.getBeanFactory()
                        .setConversionService(ApplicationConversionService.getSharedInstance()))
                .withBean(DraftEnforcementDeletionService.class, () -> draftEnforcementDeletionService)
                .withBean(FeatureToggleService.class, () -> featureToggleService)
                .withBean(EnforcementDraftDeletionScheduledTask.class)
                .withPropertyValues(
                        "enforcement-draft-deletion.schedule=DAILY|02:00",
                        "enforcement-draft-deletion.discard-after-days=" + DISCARD_AFTER_DAYS,
                        "enforcement-draft-deletion.request.backoff-delay-seconds=30")
                .run(context -> assertThat(context.getBean(EnforcementDraftDeletionScheduledTask.class)
                        .backoffDelay()).isEqualTo(Duration.ofSeconds(30)));
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -1})
    void shouldRefuseADiscardPeriodBelowOneDay(int discardAfterDays) {
        assertThatThrownBy(() -> new EnforcementDraftDeletionScheduledTask(
                "DAILY|02:00", discardAfterDays, MAX_RETRIES, Duration.ofSeconds(10),
                draftEnforcementDeletionService, featureToggleService))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ENFORCEMENT_DRAFT_DELETION_DISCARD_AFTER_DAYS")
                .hasMessageContaining("was " + discardAfterDays);
    }

    @ParameterizedTest
    @CsvSource({"0, false", "1, true", "30, true"})
    void shouldOnlyStartWithADiscardPeriodOfAtLeastOneDay(int discardAfterDays, boolean starts) {
        contextRunner()
                .withPropertyValues("enforcement-draft-deletion.discard-after-days=" + discardAfterDays)
                .run(context -> {
                    if (starts) {
                        assertThat(context).hasNotFailed();
                    } else {
                        assertThat(context).hasFailed();
                        assertThat(context.getStartupFailure())
                                .rootCause()
                                .isInstanceOf(IllegalArgumentException.class)
                                .hasMessageContaining("ENFORCEMENT_DRAFT_DELETION_DISCARD_AFTER_DAYS");
                    }
                });
    }

    private ApplicationContextRunner contextRunner() {
        return new ApplicationContextRunner()
                .withInitializer(context -> context.getBeanFactory()
                        .setConversionService(ApplicationConversionService.getSharedInstance()))
                .withBean(DraftEnforcementDeletionService.class, () -> draftEnforcementDeletionService)
                .withBean(FeatureToggleService.class, () -> featureToggleService)
                .withBean(EnforcementDraftDeletionScheduledTask.class)
                .withPropertyValues("enforcement-draft-deletion.schedule=DAILY|02:00");
    }

    private EnforcementDraftDeletionScheduledTask newScheduledTask(String schedule) {
        return new EnforcementDraftDeletionScheduledTask(schedule, DISCARD_AFTER_DAYS, MAX_RETRIES,
                Duration.ofSeconds(10), draftEnforcementDeletionService,
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