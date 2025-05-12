package uk.gov.hmcts.reform.pcs.scheduling;

import com.github.kagkarlsson.scheduler.task.ExecutionContext;
import com.github.kagkarlsson.scheduler.task.OnStartup;
import com.github.kagkarlsson.scheduler.task.Task;
import com.github.kagkarlsson.scheduler.task.TaskInstance;
import com.github.kagkarlsson.scheduler.task.helper.RecurringTask;
import com.github.kagkarlsson.scheduler.task.schedule.FixedDelay;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
@Slf4j
@Getter
public class TaskFactory {

    private List<Task<OnStartup>> startupTasks;
    private List<Task<?>> allTasks;

    public TaskFactory() {
        startupTasks = new ArrayList<>();
        startupTasks.add(exampleTask());
        allTasks = establishTasks();
    }

    private List<Task<?>> establishTasks() {
        List<Task<?>> tasks = new ArrayList<>();
        startupTasks = getStartupTasks();
        tasks.addAll(startupTasks);
        return new ArrayList<>(tasks.stream()
                .collect(Collectors.toMap(
                        Task::getName,
                        task -> task,
                        (existing, replacement) -> {
                            log.warn("Duplicate task name found: {} - keeping existing and ignoring replacement",
                                    existing.getName());
                            return existing;
                        },
                        LinkedHashMap::new))
                .values());
    }

    private Task<OnStartup> exampleTask() {
        return new AnExampleRecurringTask("exampleTask-" + UUID.randomUUID());
    }

    private static class AnExampleRecurringTask extends RecurringTask<OnStartup> implements OnStartup {
        public AnExampleRecurringTask(String name) {
            super(name, FixedDelay.of(Duration.ofSeconds(1)), OnStartup.class);
        }

        @Override
        public void executeRecurringly(TaskInstance<OnStartup> taskInstance, ExecutionContext executionContext) {
            log.info("Running test task: {}", taskInstance.getTaskName());
        }
    }

}
