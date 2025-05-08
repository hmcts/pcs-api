package uk.gov.hmcts.reform.pcs.config;

import com.github.kagkarlsson.scheduler.Scheduler;
import com.github.kagkarlsson.scheduler.task.helper.RecurringTask;
import com.github.kagkarlsson.scheduler.task.helper.Tasks;
import com.github.kagkarlsson.scheduler.task.schedule.FixedDelay;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import javax.sql.DataSource;
import java.time.Duration;
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
    public Scheduler testScheduler(DataSource dataSource, CountDownLatch countDownLatch) {
        String uniqueTaskName = "test-task-" + UUID.randomUUID();

        RecurringTask<?> task = Tasks.recurring(uniqueTaskName, FixedDelay.of(Duration.ofSeconds(1)))
                .execute((taskInstance, executionContext) -> {
                    long countBefore = countDownLatch.getCount();
                    log.info("Running test task: {} - with count of {}", taskInstance.getTaskName(), countBefore);
                    countDownLatch.countDown();
                });

        return Scheduler.create(dataSource).pollingInterval(Duration.ofSeconds(1))
                .registerShutdownHook().startTasks(task).build();
    }

}
