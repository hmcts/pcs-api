package uk.gov.hmcts.reform.pcs.ccd.task;

import com.github.kagkarlsson.scheduler.task.CompletionHandler;
import com.github.kagkarlsson.scheduler.task.FailureHandler;
import com.github.kagkarlsson.scheduler.task.TaskDescriptor;
import com.github.kagkarlsson.scheduler.task.helper.CustomTask;
import com.github.kagkarlsson.scheduler.task.helper.Tasks;
import lombok.AllArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.pcs.ccd.model.OrderDocumentTaskData;
import uk.gov.hmcts.reform.pcs.ccd.service.order.OrderDocumentService;

import java.time.Duration;

/** Generates an issued order's document once the event that issued it has committed. */
@Component
@AllArgsConstructor
public class OrderDocumentGenerationComponent {

    public static final TaskDescriptor<OrderDocumentTaskData> ORDER_DOCUMENT_TASK_DESCRIPTOR =
        TaskDescriptor.of("order-document-generation", OrderDocumentTaskData.class);

    private static final int MAX_RETRIES = 5;
    private static final Duration RETRY_AFTER = Duration.ofMinutes(1);

    private final OrderDocumentService orderDocumentService;

    @Bean
    public CustomTask<OrderDocumentTaskData> orderDocumentGenerationTask() {
        return Tasks.custom(ORDER_DOCUMENT_TASK_DESCRIPTOR)
            .onFailure(new FailureHandler.MaxRetriesFailureHandler<>(MAX_RETRIES,
                new FailureHandler.ExponentialBackoffFailureHandler<>(RETRY_AFTER)))
            .execute((instance, context) -> {
                orderDocumentService.generateAndAttach(instance.getData());
                return new CompletionHandler.OnCompleteRemove<>();
            });
    }
}
