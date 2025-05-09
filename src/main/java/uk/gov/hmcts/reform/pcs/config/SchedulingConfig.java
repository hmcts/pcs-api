package uk.gov.hmcts.reform.pcs.config;

import com.github.kagkarlsson.scheduler.Scheduler;
import com.github.kagkarlsson.scheduler.SchedulerClient;
import com.github.kagkarlsson.scheduler.task.Task;
import com.github.kagkarlsson.scheduler.task.helper.CustomTask;
import com.github.kagkarlsson.scheduler.task.helper.RecurringTask;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import javax.sql.DataSource;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Configuration
@Slf4j
public class SchedulingConfig {

    @Bean
    public SchedulerClient schedulerClient(DataSource dataSource, List<Task<?>> tasks) {
        // Allows for interaction with the Scheduling system without actually executing the tasks.
        return SchedulerClient.Builder.create(dataSource, tasks).build();
    }

    @Bean(initMethod = "start", destroyMethod = "stop")
    public Scheduler recurringTasksScheduler(DataSource dataSource,
                                             List<RecurringTask<?>> tasks,
                                             @Value("${scheduler.polling.interval:1}") long interval) {
        log.debug("Incoming tasks size: {}", tasks.size());
        tasks.forEach(task -> log.debug("Task name: {}", task.getName()));

        List<RecurringTask<?>> uniqueTasks = new ArrayList<>();
        tasks.stream()
                .collect(Collectors.groupingBy(Task::getName))
                .forEach((name, taskList) -> {
                    log.debug("Processing task name: {} with {} instances", name, taskList.size());
                    uniqueTasks.add(taskList.get(0));
                });

        log.debug("Final unique tasks size: {}", uniqueTasks.size());

        return Scheduler
                .create(dataSource, uniqueTasks.toArray(new RecurringTask<?>[0]))
                .pollingInterval(Duration.ofSeconds(interval))
                .startTasks(uniqueTasks)
                .build();
    }

    @Bean(destroyMethod = "stop")
    public Scheduler customTasksScheduler(DataSource dataSource,
                                    List<CustomTask<?>> tasks,
                                    @Value("${scheduler.polling.interval:10}") long interval) {
        return Scheduler
                .create(dataSource, tasks.toArray(new CustomTask<?>[0]))
                .pollingInterval(Duration.ofSeconds(interval))
                .startTasks(tasks)
                .build();
    }

}
