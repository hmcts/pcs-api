package uk.gov.hmcts.reform.pcs.notify.listener;

import com.github.kagkarlsson.scheduler.SchedulerClient;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PostUpdate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.pcs.ccd.entity.feesandpay.FeePaymentEntity;
import uk.gov.hmcts.reform.pcs.ccd.model.FeePaymentStatusChangeTaskData;
import uk.gov.hmcts.reform.pcs.ccd.task.FeePaymentPaidNotificationTaskComponent;
import uk.gov.hmcts.reform.pcs.feesandpay.model.PaymentCallbackHandlerType;
import uk.gov.hmcts.reform.pcs.feesandpay.model.PaymentStatus;

import java.time.Instant;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class FeePaymentEntityListener {

    private final SchedulerClient schedulerClient;

    @PostLoad
    public void onPostLoad(FeePaymentEntity entity) {
        entity.setPreviousPaymentStatus(entity.getPaymentStatus());
    }

    @PostUpdate
    public void onPostUpdate(FeePaymentEntity entity) {
        if (entity.getPaymentStatus() == entity.getPreviousPaymentStatus()) {
            return;
        }

        if (entity.getParty() == null) {
            log.warn(
                "Skipping fee payment notification because no party is associated with fee payment {}",
                entity.getId()
            );
            return;
        }

        Integer feePaymentId = entity.getId();

        boolean isClaimIssueFeePayment =
            entity.getPaymentCallbackHandlerType() == PaymentCallbackHandlerType.CLAIM;

        if (entity.getPaymentStatus() == PaymentStatus.PAID && isClaimIssueFeePayment) {
            String taskId = UUID.randomUUID().toString();
            log.info("Scheduling fee payment paid notification for: {}, with task id: {}",
                     feePaymentId,
                     taskId);

            schedulerClient.scheduleIfNotExists(
                FeePaymentPaidNotificationTaskComponent.FEE_PAYMENT_PAID_TASK_DESCRIPTOR
                    .instance(taskId)
                    .data(FeePaymentStatusChangeTaskData.builder()
                              .feePaymentId(feePaymentId)
                              .build())
                    .scheduledTo(Instant.now())
            );
            return;
        }

        log.info(
            "Skipping fee payment paid notification for fee payment {} (status={}, handlerType={})",
            feePaymentId,
            entity.getPaymentStatus(),
            entity.getPaymentCallbackHandlerType()
        );
    }
}
