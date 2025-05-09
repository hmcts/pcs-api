package uk.gov.hmcts.reform.pcs.config;

import com.github.kagkarlsson.scheduler.Scheduler;
import com.github.kagkarlsson.scheduler.SchedulerClient;
import com.github.kagkarlsson.scheduler.task.OnStartup;
import com.github.kagkarlsson.scheduler.task.Task;
import com.github.kagkarlsson.scheduler.task.helper.Tasks;
import com.github.kagkarlsson.scheduler.task.schedule.FixedDelay;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.Primary;

import javax.sql.DataSource;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;

@TestConfiguration
@Slf4j
public class TestSchedulingConfig {

    @Bean
    public CountDownLatch countDownLatch() {
        return new CountDownLatch(10);
    }

    @Bean
    @Primary
    @DependsOn("testSchedulerClient")
    public Scheduler testScheduler(DataSource dataSource, Task<OnStartup> testTask) {
        final Scheduler scheduler = Scheduler.create(dataSource)
                .pollingInterval(Duration.ofSeconds(1))
                .startTasks((Task<OnStartup> & OnStartup) testTask)
                .registerShutdownHook()
                .build();
        scheduler.start();
        return scheduler;
    }

    @Bean
    @Primary
    public SchedulerClient testSchedulerClient(DataSource dataSource, Task<OnStartup> testTask) {
        final SchedulerClient schedulerClient = SchedulerClient.Builder.create(dataSource).build();
        schedulerClient.scheduleIfNotExists(testTask.instance(testTask.getName()), Instant.now().plusSeconds(3));
        return schedulerClient;
    }

    @Bean
    public String taskName() {
        return "test-task-" + UUID.randomUUID();
    }

    @Bean
    public Task<?> testTask(CountDownLatch countDownLatch, String taskName) {
        return Tasks.recurring(taskName, FixedDelay.of(Duration.ofSeconds(1)))
            .execute((taskInstance, executionContext) -> {
                long countBefore = countDownLatch.getCount();
                log.info("Running test task: {} - with count of {}", taskInstance.getTaskName(), countBefore);
                countDownLatch.countDown();
            });
    }


}