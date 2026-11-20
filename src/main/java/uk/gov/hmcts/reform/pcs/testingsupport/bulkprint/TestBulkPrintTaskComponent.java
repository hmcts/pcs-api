package uk.gov.hmcts.reform.pcs.testingsupport.bulkprint;

import com.github.kagkarlsson.scheduler.task.FailureHandler;
import com.github.kagkarlsson.scheduler.task.TaskDescriptor;
import com.github.kagkarlsson.scheduler.task.helper.OneTimeTask;
import com.github.kagkarlsson.scheduler.task.helper.Tasks;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * Runs the test support bulk print letter after the event that asked for it has committed, by which time CCD has
 * attached the uploaded documents to the case in CDAM. A failure is retried a few times, then dropped: this is a
 * test aid, and its log says why.
 */
@Slf4j
@Component
public class TestBulkPrintTaskComponent {

    public static final String TEST_BULK_PRINT_TASK_NAME = "test-bulk-print-task";
    public static final TaskDescriptor<TestBulkPrintTaskData> TEST_BULK_PRINT_TASK =
        TaskDescriptor.of(TEST_BULK_PRINT_TASK_NAME, TestBulkPrintTaskData.class);

    private static final int MAX_RETRIES = 3;

    private final TestBulkPrintSender sender;

    public TestBulkPrintTaskComponent(TestBulkPrintSender sender) {
        this.sender = sender;
    }

    @Bean
    public OneTimeTask<TestBulkPrintTaskData> testBulkPrintTask() {
        return Tasks.oneTime(TEST_BULK_PRINT_TASK)
            .onFailure(new FailureHandler.MaxRetriesFailureHandler<>(MAX_RETRIES,
                new FailureHandler.ExponentialBackoffFailureHandler<>(Duration.ofSeconds(10))))
            .execute((taskInstance, executionContext) ->
                sender.post(sender.resolveClaimant(taskInstance.getData())));
    }
}
