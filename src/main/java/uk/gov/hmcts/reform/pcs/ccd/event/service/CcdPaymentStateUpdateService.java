package uk.gov.hmcts.reform.pcs.ccd.event.service;

import com.github.kagkarlsson.scheduler.SchedulerClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.sdk.SystemEventExecutionResult;
import uk.gov.hmcts.ccd.sdk.SystemEventExecutor;
import uk.gov.hmcts.reform.pcs.ccd.event.EventId;
import uk.gov.hmcts.reform.pcs.ccd.event.action.ClaimPaymentAction;
import uk.gov.hmcts.reform.pcs.ccd.event.action.EnforcementPaymentAction;
import uk.gov.hmcts.reform.pcs.ccd.event.action.PaymentSuccessAction;
import uk.gov.hmcts.reform.pcs.feesandpay.model.PaymentStateTaskData;
import uk.gov.hmcts.reform.pcs.feesandpay.model.PaymentStatusCallback;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static uk.gov.hmcts.reform.pcs.feesandpay.task.CcdPaymentUpdateTaskComponent.CCD_PAYMENT_UPDATE_TASK_DESCRIPTOR;

@Service
@Slf4j
public class CcdPaymentStateUpdateService {

    private final SchedulerClient schedulerClient;
    private final SystemEventExecutor systemEventExecutor;
    private final Map<EventId, PaymentSuccessAction> paymentSuccessActions;

    public CcdPaymentStateUpdateService(SchedulerClient schedulerClient,
                                        SystemEventExecutor systemEventExecutor,
                                        ClaimPaymentAction claimPaymentAction,
                                        EnforcementPaymentAction enforcementPaymentAction) {
        this.schedulerClient = schedulerClient;
        this.systemEventExecutor = systemEventExecutor;
        this.paymentSuccessActions = new HashMap<>();
        this.paymentSuccessActions.put(claimPaymentAction.eventId(), claimPaymentAction);
        this.paymentSuccessActions.put(enforcementPaymentAction.eventId(), enforcementPaymentAction);
    }

    public void submitPaymentSuccess(long caseId, EventId eventId, PaymentStatusCallback paymentStatusCallback) {
        PaymentStateTaskData taskData = PaymentStateTaskData.builder()
            .caseReference(caseId).eventId(eventId).paymentReference(paymentStatusCallback.getPaymentReference())
            .idempotencyKey(UUID.randomUUID()).build();
        String instanceId = eventId.name() + "-" + caseId;
        schedulerClient.scheduleIfNotExists(CCD_PAYMENT_UPDATE_TASK_DESCRIPTOR
                .instance(instanceId).data(taskData).scheduledTo(Instant.now()));
    }

    public void updateCcd(PaymentStateTaskData taskData) {
        log.info("Updating the CCD State for Case: {} in Event: {} and PaymentReference: {}",
                 taskData.getCaseReference(), taskData.getEventId(), taskData.getPaymentReference());
        SystemEventExecutionResult result = systemEventExecutor.execute(taskData.getCaseReference(),
           taskData.getIdempotencyKey(), context ->
                paymentSuccessActions.get(taskData.getEventId()).apply(taskData));
        log.info("System Event from Payment Callback event resulted in : {}", result);
    }

}
