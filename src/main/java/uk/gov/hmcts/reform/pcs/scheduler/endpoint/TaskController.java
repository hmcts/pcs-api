package uk.gov.hmcts.reform.pcs.scheduler.endpoint;

import com.github.kagkarlsson.scheduler.Scheduler;
import com.github.kagkarlsson.scheduler.task.TaskInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.reform.pcs.scheduler.config.HelloWorldTaskConfig;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@RestController
@RequestMapping("/scheduler")
public class TaskController {

    private final Scheduler scheduler;

    @Autowired
    public TaskController(Scheduler scheduler) {
        this.scheduler = scheduler;
    }

    @PostMapping("/hello-world")
    public ResponseEntity<String> scheduleHelloWorldTask(
        @RequestParam(required = false, defaultValue = "30") Integer delaySeconds) {

        // Create a unique instance ID
        String instanceId = "instance-" + Instant.now().toEpochMilli();

        // Schedule the task to run after the specified delay
        Instant executionTime = Instant.now().plus(delaySeconds, ChronoUnit.SECONDS);

        // Create and schedule the task instance
        TaskInstance<Void> taskInstance =
            new TaskInstance<>(HelloWorldTaskConfig.TASK_NAME, instanceId);

        scheduler.schedule(taskInstance, executionTime);

        return ResponseEntity.ok("Hello World task scheduled with ID: " + instanceId
                                    + " to run at: " + executionTime);
    }
}
