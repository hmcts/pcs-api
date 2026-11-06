package uk.gov.hmcts.reform.pcs.ccd.event.service;

import com.github.kagkarlsson.scheduler.SchedulerClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.sdk.SystemEventExecutionResult;
import uk.gov.hmcts.ccd.sdk.SystemEventExecutor;
import uk.gov.hmcts.reform.pcs.ccd.event.EventId;
import uk.gov.hmcts.reform.pcs.ccd.event.action.PaymentSuccessAction;
import uk.gov.hmcts.reform.pcs.system.SystemAction;
import uk.gov.hmcts.reform.pcs.feesandpay.model.PaymentStateTaskData;
import uk.gov.hmcts.reform.pcs.feesandpay.model.PaymentStatusCallback;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import static uk.gov.hmcts.reform.pcs.feesandpay.task.CcdPaymentUpdateTaskComponent.CCD_PAYMENT_UPDATE_TASK_DESCRIPTOR;

@Service
@Slf4j
public class CcdPaymentStateUpdateService {

    private final SchedulerClient schedulerClient;
    private final SystemEventExecutor systemEventExecutor;
    private final Map<SystemAction, PaymentSuccessAction> paymentSuccessActions;

    public CcdPaymentStateUpdateService(SchedulerClient schedulerClient,
                                        SystemEventExecutor systemEventExecutor,
                                        List<PaymentSuccessAction> paymentSuccessActions) {
        this.schedulerClient = schedulerClient;
        this.systemEventExecutor = systemEventExecutor;
        this.paymentSuccessActions = paymentSuccessActions.stream()
            .collect(Collectors.toUnmodifiableMap(PaymentSuccessAction::systemAction, Function.identity()));
    }

    public void submitPaymentSuccess(long caseId, EventId eventId, PaymentStatusCallback paymentStatusCallback,
                                     SystemAction systemAction) {
        PaymentStateTaskData taskData = PaymentStateTaskData.builder()
            .caseReference(caseId)
            .eventId(eventId)
            .systemAction(systemAction)
            .paymentReference(paymentStatusCallback.getPaymentReference())
            .idempotencyKey(UUID.randomUUID()).build();
        String instanceId = systemAction.name() + "-" + paymentStatusCallback.getPaymentReference();
        schedulerClient.scheduleIfNotExists(CCD_PAYMENT_UPDATE_TASK_DESCRIPTOR
                .instance(instanceId).data(taskData).scheduledTo(Instant.now()));
    }

    public void updateCcd(PaymentStateTaskData taskData) {
        log.info("Updating the CCD State for Case: {} in Event: {} and PaymentReference: {}",
                 taskData.getCaseReference(), taskData.getEventId(), taskData.getPaymentReference());
        SystemEventExecutionResult result = systemEventExecutor.execute(taskData.getCaseReference(),
            taskData.getIdempotencyKey(), context ->
                paymentSuccessActions.get(taskData.getSystemAction()).apply(taskData));
        log.info("System Event from Payment Callback event resulted in : {}", result);
    }



}
