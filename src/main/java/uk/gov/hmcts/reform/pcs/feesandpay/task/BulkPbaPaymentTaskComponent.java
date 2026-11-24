package uk.gov.hmcts.reform.pcs.feesandpay.task;

import com.github.kagkarlsson.scheduler.task.CompletionHandler;
import com.github.kagkarlsson.scheduler.task.FailureHandler;
import com.github.kagkarlsson.scheduler.task.TaskDescriptor;
import com.github.kagkarlsson.scheduler.task.helper.CustomTask;
import com.github.kagkarlsson.scheduler.task.helper.Tasks;
import feign.FeignException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.payments.client.InvalidPaymentRequestException;
import uk.gov.hmcts.reform.pcs.feesandpay.model.BulkPbaPaymentTaskData;
import uk.gov.hmcts.reform.pcs.feesandpay.service.BulkPaymentStatusListener;
import uk.gov.hmcts.reform.pcs.feesandpay.service.PaymentService;

import java.time.Duration;
import java.util.Set;

@Slf4j
@Component
public class BulkPbaPaymentTaskComponent {

    private static final String BULK_PBA_PAYMENT_TASK_NAME = "bulk-pba-payment-task";

    public static final TaskDescriptor<BulkPbaPaymentTaskData> BULK_PBA_PAYMENT_TASK_DESCRIPTOR =
        TaskDescriptor.of(BULK_PBA_PAYMENT_TASK_NAME, BulkPbaPaymentTaskData.class);

    private static final Set<Integer> FINAL_REJECTION_STATUSES = Set.of(400, 402, 404, 410, 412, 422);

    private final PaymentService paymentService;
    private final BulkPaymentStatusListener statusListener;
    private final int maxRetries;
    private final Duration backoffDelay;

    public BulkPbaPaymentTaskComponent(
        PaymentService paymentService,
        BulkPaymentStatusListener statusListener,
        @Value("${fees.request.max-retries}") int maxRetries,
        @Value("${fees.request.backoff-delay-seconds}") Duration backoffDelay
    ) {
        this.paymentService = paymentService;
        this.statusListener = statusListener;
        this.maxRetries = maxRetries;
        this.backoffDelay = backoffDelay;
    }

    /**
     * One-time task paying a claim issue fee from a PBA account. Definite rejections (400, 402, 404, 410,
     * 412, 422) are final. A 409 means the payment is already in progress or made for this service request, so it is
     * treated as submitted and the payment callback settles it. Any other failure, including 401, 403, 408, 425, 429,
     * server errors, IO errors and timeouts, is retried with exponential backoff up to the configured maximum, after
     * which the payment is reported as failed. Each attempt uses a fresh idempotency key. A duplicate payment
     * response is treated as success.
     */
    @Bean
    public CustomTask<BulkPbaPaymentTaskData> bulkPbaPaymentTask() {
        FailureHandler<BulkPbaPaymentTaskData> backoff =
            new FailureHandler.ExponentialBackoffFailureHandler<>(backoffDelay);

        return Tasks.custom(BULK_PBA_PAYMENT_TASK_DESCRIPTOR)
            .onFailure((executionComplete, executionOperations) -> {
                int totalFailures = executionComplete.getExecution().consecutiveFailures + 1;
                if (totalFailures > maxRetries) {
                    BulkPbaPaymentTaskData data = (BulkPbaPaymentTaskData) executionComplete.getExecution()
                        .taskInstance.getData();
                    log.error("Giving up PBA payment for service request {} after {} attempts",
                              data.getServiceRequestReference(), totalFailures);
                    notifyFailed(data, "Payment could not be submitted after " + totalFailures + " attempts");
                    executionOperations.remove();
                } else {
                    backoff.onFailure(executionComplete, executionOperations);
                }
            })
            .execute((taskInstance, executionContext) -> {
                BulkPbaPaymentTaskData data = taskInstance.getData();
                try {
                    paymentService.createSystemPbaPayment(data);
                } catch (InvalidPaymentRequestException e) {
                    log.info("Duplicate PBA payment for service request {}, treating as submitted",
                             data.getServiceRequestReference());
                } catch (FeignException e) {
                    if (e.status() == 409) {
                        log.info("PBA payment for service request {} is already in progress or made, "
                                     + "treating as submitted", data.getServiceRequestReference());
                        statusListener.onPbaPaymentSubmitted(data.getBulkClaimSubmissionId(),
                                                             data.getServiceRequestReference());
                        return new CompletionHandler.OnCompleteRemove<>();
                    }
                    if (isDefiniteRejection(e)) {
                        log.error("PBA payment rejected for service request {} with status {}",
                                  data.getServiceRequestReference(), e.status(), e);
                        notifyFailed(data, "Payment rejected with status " + e.status() + ": " + e.contentUTF8());
                        return new CompletionHandler.OnCompleteRemove<>();
                    }
                    throw e;
                }
                statusListener.onPbaPaymentSubmitted(data.getBulkClaimSubmissionId(),
                                                     data.getServiceRequestReference());
                return new CompletionHandler.OnCompleteRemove<>();
            });
    }

    private static boolean isDefiniteRejection(FeignException e) {
        return FINAL_REJECTION_STATUSES.contains(e.status());
    }

    private void notifyFailed(BulkPbaPaymentTaskData data, String reason) {
        statusListener.onPbaPaymentFailed(data.getBulkClaimSubmissionId(), data.getServiceRequestReference(), reason);
    }

}
