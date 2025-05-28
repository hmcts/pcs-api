package uk.gov.hmcts.reform.pcs.config;

import com.github.kagkarlsson.scheduler.Scheduler;
import com.github.kagkarlsson.scheduler.SchedulerClient;
import com.github.kagkarlsson.scheduler.task.Task;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;

import javax.sql.DataSource;
import java.time.Duration;
import java.util.List;

@Configuration
@Slf4j
@AllArgsConstructor
public class SchedulingConfig {


    /**
     * SchedulerClient bean is always and this is  used to schedule jobs, but does NOT execute them.
     * Keep active everywhere where job scheduling is
     * needed.
     */
    @Bean
    public SchedulerClient schedulerClient(DataSource dataSource) {
        return SchedulerClient.Builder.create(dataSource).build();
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
                                           @Value("${job.polling-interval}") long interval, List<Task<?>> tasks) {
        Scheduler scheduler = Scheduler.create(dataSource, tasks)
                .pollingInterval(Duration.ofSeconds(interval))
                .registerShutdownHook()
                .build();
        scheduler.start();
        return scheduler;
    }
}
