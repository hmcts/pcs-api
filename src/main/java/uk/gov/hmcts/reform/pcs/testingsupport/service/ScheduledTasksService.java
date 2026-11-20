package uk.gov.hmcts.reform.pcs.testingsupport.service;

import com.github.kagkarlsson.scheduler.Scheduler;
import com.github.kagkarlsson.scheduler.SchedulerClient;
import com.github.kagkarlsson.scheduler.exceptions.TaskInstanceCurrentlyExecutingException;
import com.github.kagkarlsson.scheduler.task.TaskInstanceId;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

@Slf4j
@Service
@ConditionalOnProperty(name = "testing-support.enabled", havingValue = "true")
public class ScheduledTasksService {

    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final SchedulerClient schedulerClient;
    private final Scheduler scheduler;
    private final Clock utcClock;

    public ScheduledTasksService(NamedParameterJdbcTemplate jdbcTemplate,
                                 SchedulerClient schedulerClient,
                                 Scheduler scheduler,
                                 @Qualifier("utcClock") Clock utcClock) {
        this.jdbcTemplate = jdbcTemplate;
        this.schedulerClient = schedulerClient;
        this.scheduler = scheduler;
        this.utcClock = utcClock;
    }

    public void executeFutureDbSchedulerTasks(String taskName, Duration windowDuration) {
        Instant windowEnd = Instant.now(utcClock).plus(windowDuration);

        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("taskName", taskName);
        params.addValue("windowEnd", Timestamp.from(windowEnd));

        List<String> taskIds = jdbcTemplate
            .queryForList("""
                                  SELECT task_instance
                                  FROM scheduled_tasks
                                  WHERE task_name = :taskName AND execution_time <= :windowEnd
                              """, params, String.class);

        if (taskIds.isEmpty()) {
            log.warn("No tasks of name {} found", taskName);
        }

        for (String taskId : taskIds) {
            rescheduleWhenIdle(taskName, taskId);
        }

        scheduler.triggerCheckForDueExecutions();
    }

    /**
     * Waits for a currently-executing instance to finish, then moves it due now. Bulk print conversion can hold the
     * recurring task for tens of seconds; skipping the reschedule left later pack asserts waiting forever.
     */
    private void rescheduleWhenIdle(String taskName, String taskId) {
        Instant deadline = Instant.now().plus(Duration.ofMinutes(2));
        TaskInstanceId instanceId = TaskInstanceId.of(taskName, taskId);
        while (true) {
            try {
                schedulerClient.reschedule(instanceId, Instant.now(utcClock));
                log.info("Rescheduled task with ID: {}", taskId);
                return;
            } catch (TaskInstanceCurrentlyExecutingException e) {
                if (!Instant.now().isBefore(deadline)) {
                    throw new IllegalStateException(
                        "Timed out waiting to reschedule task %s %s still executing".formatted(taskName, taskId), e);
                }
                log.warn("Task {} {} is already running - waiting to reschedule", taskName, taskId);
                sleepBriefly();
            }
        }
    }

    private static void sleepBriefly() {
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while waiting to reschedule a db-scheduler task", e);
        }
    }

}
