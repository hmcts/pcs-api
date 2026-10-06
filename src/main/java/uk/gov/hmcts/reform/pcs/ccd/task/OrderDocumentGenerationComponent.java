package uk.gov.hmcts.reform.pcs.ccd.task;

import com.github.kagkarlsson.scheduler.task.CompletionHandler;
import com.github.kagkarlsson.scheduler.task.FailureHandler;
import com.github.kagkarlsson.scheduler.task.TaskDescriptor;
import com.github.kagkarlsson.scheduler.task.helper.CustomTask;
import com.github.kagkarlsson.scheduler.task.helper.Tasks;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.pcs.ccd.model.OrderDocumentTaskData;
import uk.gov.hmcts.reform.pcs.ccd.service.order.OrderDocumentService;

import java.time.Duration;
import java.util.UUID;

/**
 * db-scheduler {@code CustomTask} bean that generates an issued order's document, with the claim
 * form's retry shape: {@link ClaimFormGenerationComponent}.
 */
@Slf4j
@Component
public class OrderDocumentGenerationComponent {

    public static final TaskDescriptor<OrderDocumentTaskData> ORDER_DOCUMENT_TASK_DESCRIPTOR =
        TaskDescriptor.of("order-document-generation-task", OrderDocumentTaskData.class);

    private final OrderDocumentService orderDocumentService;
    private final int maxRetries;
    private final Duration backoffDelay;

    public OrderDocumentGenerationComponent(
        OrderDocumentService orderDocumentService,
        @Value("${order-document.request.max-retries}") int maxRetries,
        @Value("${order-document.request.backoff-delay-seconds}") Duration backoffDelay
    ) {
        this.orderDocumentService = orderDocumentService;
        this.maxRetries = maxRetries;
        this.backoffDelay = backoffDelay;
    }

    @Bean
    public CustomTask<OrderDocumentTaskData> orderDocumentGenerationTask() {
        return Tasks.custom(ORDER_DOCUMENT_TASK_DESCRIPTOR)
            .onFailure(new FailureHandler.MaxRetriesFailureHandler<>(
                maxRetries,
                new FailureHandler.ExponentialBackoffFailureHandler<>(backoffDelay)
            ))
            .execute((taskInstance, executionContext) -> {
                UUID issuedOrderId = UUID.fromString(taskInstance.getData().getIssuedOrderId());
                try {
                    orderDocumentService.generateAndAttach(issuedOrderId);
                    return new CompletionHandler.OnCompleteRemove<>();
                } catch (Exception e) {
                    int attempt = executionContext.getExecution().consecutiveFailures + 1;
                    if (attempt > maxRetries) {
                        log.error("Order document generation permanently failed for issued order {} after {} "
                                  + "attempts: {}", issuedOrderId, attempt, e.getMessage(), e);
                    }
                    throw e;
                }
            });
    }
}
