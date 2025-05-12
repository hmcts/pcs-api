package uk.gov.hmcts.reform.pcs.config;

import com.github.kagkarlsson.scheduler.Scheduler;
import com.github.kagkarlsson.scheduler.SchedulerClient;
import com.github.kagkarlsson.scheduler.task.OnStartup;
import com.github.kagkarlsson.scheduler.task.Task;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import uk.gov.hmcts.reform.pcs.scheduling.TaskFactory;

import javax.sql.DataSource;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Configuration
@Slf4j
@AllArgsConstructor
public class SchedulingConfig {

    private final TaskFactory taskFactory;

    /**
     * SchedulerClient bean is always present and used to schedule jobs, but does NOT execute them.
     * Keep active everywhere where job scheduling is needed.
     */
    @Bean
    public SchedulerClient schedulerClient(DataSource dataSource) {
        final List<Task<?>> allTasks = taskFactory.getAllTasks();
        final SchedulerClient schedulerClient = SchedulerClient.Builder.create(dataSource).build();
        allTasks.forEach(
                task -> schedulerClient.scheduleIfNotExists(task.instance(task.getName()),
                        Instant.now().plusSeconds(3)
                ) // Alter the second parameter according to requirements - potentially get this from the Task itself.
        );
        return schedulerClient;
    }

    /**
     * Scheduler bean is conditionally created and started ONLY if job execution is enabled.
     * Use environment variable "job.executor.enabled" = true to enable execution.
     * Only start with on nodes intended for job execution. Scheduler.start()
     * Share the same database for job metadata for coordination. (if multiple databases are used in the future)
     * Ensure that instances have graceful shutdown hooks to prevent missed executions.
     */
    @Bean(initMethod = "start", destroyMethod = "stop")
    @ConditionalOnProperty(name = "job.executor.enabled", havingValue = "true")
    @DependsOn("schedulerClient")
    public Scheduler startupTasksScheduler(DataSource dataSource,
                                           @Value("${scheduler.polling.interval:1}") long interval) {
        List<Task<OnStartup>> startupTasks = taskFactory.getStartupTasks();
        Scheduler scheduler = Scheduler.create(dataSource)
                .pollingInterval(Duration.ofSeconds(interval))
                .registerShutdownHook()
                .startTasks(toOnStartupArray(new ArrayList<>(startupTasks)))
                .build();
        scheduler.start();
        return scheduler;
    }

    @SuppressWarnings("unchecked")
    private static <T extends Task<?> & OnStartup> T[] toOnStartupArray(List<Task<?>> tasks) {
        return tasks.stream()
                .filter(OnStartup.class::isInstance)
                .map(task -> (T) task)
                .toArray(size -> (T[]) new Task<?>[size]);
    }

}
