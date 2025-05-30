package uk.gov.hmcts.reform.pcs.config;

import com.github.kagkarlsson.scheduler.Scheduler;
import com.github.kagkarlsson.scheduler.SchedulerClient;
import com.github.kagkarlsson.scheduler.event.ExecutionInterceptor;
import com.github.kagkarlsson.scheduler.event.SchedulerListener;
import com.github.kagkarlsson.scheduler.task.Task;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.Primary;

import javax.sql.DataSource;
import java.time.Duration;
import java.util.List;

@Configuration
@Slf4j
@AllArgsConstructor
public class SchedulingConfig {


    /**
     * SchedulerClient bean is always on and this is  used to schedule jobs, but does NOT execute them.
     * Keep active everywhere where job scheduling is needed.
     */
    @Bean
    @Primary
    public SchedulerClient schedulerClient(DataSource dataSource) {
        return SchedulerClient.Builder.create(dataSource).build();
    }

    /**
     * Scheduler bean is conditionally created and started ONLY if job execution is enabled.
     * Use environment variable "job.executor.enabled" = true to enable execution.
     * Only start with on nodes intended for job execution. Scheduler.start()
     * Share the same database for job metadata for coordination.
     * Worth noting two instances of the API are in use now, one called java, one called dbTaskRunner.
     * In order to make sure a task is registered by the scheduler, please use a bean that returns type Task<?>
     * Where ? Can be any type.
     */
    @Bean(initMethod = "start", destroyMethod = "stop")
    @ConditionalOnProperty(prefix = "db-scheduler", name = "executor-enabled", havingValue = "true")
    @DependsOn("schedulerClient")
    public Scheduler startupTasksScheduler(DataSource dataSource,
                                            @Value("${db-scheduler.threads}")
                                            int threadCount,
                                            @Value("${db-scheduler.polling-interval-seconds}")
                                            long interval,
                                            List<Task<?>> tasks,
                                            List<SchedulerListener> schedulerListeners,
                                            List<ExecutionInterceptor> executionInterceptors) {
        log.info("Starting scheduler");

        var builder = Scheduler.create(dataSource, tasks)
            .threads(threadCount)
            .pollingInterval(Duration.ofSeconds(interval))
            .registerShutdownHook();

        schedulerListeners.forEach(builder::addSchedulerListener);
        executionInterceptors.forEach(builder::addExecutionInterceptor);

        Scheduler scheduler = builder.build();
        scheduler.start();
        return scheduler;
    }
}
