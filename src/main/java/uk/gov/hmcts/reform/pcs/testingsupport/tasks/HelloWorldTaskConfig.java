package uk.gov.hmcts.reform.pcs.testingsupport.tasks;

import com.github.kagkarlsson.scheduler.task.FailureHandler;
import com.github.kagkarlsson.scheduler.task.Task;
import com.github.kagkarlsson.scheduler.task.helper.Tasks;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.context.annotation.Bean;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class HelloWorldTaskConfig {
    private static final Logger log = LoggerFactory.getLogger(HelloWorldTaskConfig.class);

    public static final String TASK_NAME = "hello-world-task";

    // Counter to track execution attempts for demonstration purposes
    private final AtomicInteger attemptCounter = new AtomicInteger(0);

    @Bean
    public Task<Void> helloWorldTask() {
        return Tasks.oneTime(TASK_NAME)
            .onFailure(new FailureHandler.OnFailureRetryLater<>(Duration.ofSeconds(3)))
            .execute((taskInstance, executionContext) -> {
                int attempt = attemptCounter.incrementAndGet();
                log.info("Hello World task execution attempt {}, Instance: {}", attempt, taskInstance.getId());

                // Simulate a failure for the first 3 attempts
                if (attempt <= 3) {
                    log.info("Simulating task failure for attempt {}", attempt);
                    throw new RuntimeException("Simulated failure for demonstration");
                }

                // If we reach here, the task is successful
                log.info("Hello World task successfully executed on attempt {}", attempt);
                // Reset counter for next task
                attemptCounter.set(0);
            });
    }
}
