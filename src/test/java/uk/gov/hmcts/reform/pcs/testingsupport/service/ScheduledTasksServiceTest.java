package uk.gov.hmcts.reform.pcs.testingsupport.service;

import com.github.kagkarlsson.scheduler.Scheduler;
import com.github.kagkarlsson.scheduler.SchedulerClient;
import com.github.kagkarlsson.scheduler.task.TaskInstanceId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;

import java.sql.Timestamp;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

import static java.time.Month.OCTOBER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ScheduledTasksServiceTest {

    private static final Instant TEST_INSTANT = LocalDate.of(2026, OCTOBER, 10)
        .atTime(12, 51, 19)
        .toInstant(ZoneOffset.UTC);

    @Mock
    private NamedParameterJdbcTemplate jdbcTemplate;
    @Mock
    private SchedulerClient schedulerClient;
    @Mock
    private Scheduler scheduler;
    @Mock
    private Clock utcClock;

    private ScheduledTasksService underTest;

    @BeforeEach
    void setUp() {
        when(utcClock.instant()).thenReturn(TEST_INSTANT);

        var beans = new DefaultListableBeanFactory();
        beans.registerSingleton("scheduler", scheduler);
        underTest = new ScheduledTasksService(jdbcTemplate, schedulerClient,
            beans.getBeanProvider(Scheduler.class), utcClock);
    }

    @Test
    void shouldRescheduleTasks() {
        // Given
        String taskId = "db-task-id";
        String taskName = "test-task";
        Duration windowDuration = Duration.parse("PT1H");

        when(jdbcTemplate.queryForList(anyString(), any(SqlParameterSource.class), eq(String.class)))
            .thenReturn(List.of(taskId));

        // When
        underTest.executeFutureDbSchedulerTasks(taskName, windowDuration);

        // Then
        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<SqlParameterSource> sqlParameterSourceCaptor = ArgumentCaptor.forClass(SqlParameterSource.class);
        ArgumentCaptor<TaskInstanceId> taskInstanceIdCaptor = ArgumentCaptor.forClass(TaskInstanceId.class);
        ArgumentCaptor<Instant> instantCaptor = ArgumentCaptor.forClass(Instant.class);

        String expectedSql = """
            SELECT task_instance
                FROM scheduled_tasks
                WHERE task_name = :taskName AND execution_time <= :windowEnd
            """;
        Instant expectedWindowEnd = TEST_INSTANT.plus(windowDuration);

        verify(jdbcTemplate).queryForList(sqlCaptor.capture(), sqlParameterSourceCaptor.capture(), eq(String.class));
        assertThat(sqlCaptor.getValue().trim()).isEqualTo(expectedSql.trim());
        SqlParameterSource sqlParameterSource = sqlParameterSourceCaptor.getValue();
        assertThat(sqlParameterSource.getValue("taskName")).isEqualTo(taskName);
        assertThat(sqlParameterSource.getValue("windowEnd")).isEqualTo(Timestamp.from(expectedWindowEnd));

        verify(schedulerClient).reschedule(taskInstanceIdCaptor.capture(), instantCaptor.capture());
        TaskInstanceId capturedTaskInstanceId = taskInstanceIdCaptor.getValue();
        Instant scheduledInstant = instantCaptor.getValue();
        assertThat(capturedTaskInstanceId.getId()).isSameAs(taskId);
        assertThat(capturedTaskInstanceId.getTaskName()).isSameAs(taskName);
        assertThat(scheduledInstant).isEqualTo(Instant.now(utcClock));
        verify(scheduler).triggerCheckForDueExecutions();
    }

    @Test
    void shouldStartAndRescheduleTasksWithoutLocalExecutor() {
        when(jdbcTemplate.queryForList(anyString(), any(SqlParameterSource.class), eq(String.class)))
            .thenReturn(List.of("db-task-id"));

        new ApplicationContextRunner()
            .withPropertyValues("testing-support.enabled=true", "db-scheduler.executor-enabled=false")
            .withBean(NamedParameterJdbcTemplate.class, () -> jdbcTemplate)
            .withBean(SchedulerClient.class, () -> schedulerClient)
            .withBean("utcClock", Clock.class, () -> utcClock)
            .withUserConfiguration(ScheduledTasksService.class)
            .run(context -> {
                assertThat(context).hasNotFailed().hasSingleBean(ScheduledTasksService.class);
                assertThat(context).doesNotHaveBean(Scheduler.class);
                context.getBean(ScheduledTasksService.class)
                    .executeFutureDbSchedulerTasks("test-task", Duration.ofHours(1));

                verify(schedulerClient).reschedule(TaskInstanceId.of("test-task", "db-task-id"), TEST_INSTANT);
            });
    }

}
