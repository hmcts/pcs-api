package uk.gov.hmcts.reform.pcs.feesandpay.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.kagkarlsson.scheduler.SchedulerClient;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.pcs.ccd.entity.feesandpay.FeePaymentEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.event.service.CcdPaymentStateUpdateService;
import uk.gov.hmcts.reform.pcs.ccd.model.FeePaymentStatusChangeTaskData;
import uk.gov.hmcts.reform.pcs.ccd.service.party.PartyService;
import uk.gov.hmcts.reform.pcs.ccd.task.FeePaymentPaidNotificationTaskComponent;
import uk.gov.hmcts.reform.pcs.feesandpay.model.FeesAndPayTaskData;
import uk.gov.hmcts.reform.pcs.feesandpay.model.PaymentStatus;
import uk.gov.hmcts.reform.pcs.feesandpay.model.PaymentStatusCallback;

import java.io.IOException;
import java.time.Instant;
import java.util.UUID;

@AllArgsConstructor
@Component
@Slf4j
public class MakeAClaimPaymentCallbackHandler implements PaymentCallbackStrategy {

    private final CcdPaymentStateUpdateService ccdPaymentStateUpdateService;
    private final PartyService partyService;
    private final ObjectMapper objectMapper;
    private final SchedulerClient schedulerClient;

    @Override
    public void handle(PaymentStatusCallback paymentStatusCallback, FeePaymentEntity feePaymentEntity) {
        FeesAndPayTaskData feesAndPayTaskData = toFeesAndPayTaskData(feePaymentEntity);
        PartyEntity claimParty = getResponsibleParty(feesAndPayTaskData);
        feePaymentEntity.setParty(claimParty);
        if (PaymentStatus.PAID == feePaymentEntity.getPaymentStatus()) {
            ccdPaymentStateUpdateService.submitPaymentSuccess(feesAndPayTaskData.getCaseReference());
            scheduleClaimIssuedNotification(feePaymentEntity);
        } else {
            log.warn("The payment was not successful [{}] for case: {}", feePaymentEntity.getPaymentStatus(),
                     feesAndPayTaskData.getCaseReference());
        }
    }

    private void scheduleClaimIssuedNotification(FeePaymentEntity feePaymentEntity) {
        Integer feePaymentId = feePaymentEntity.getId();
        String taskId = UUID.randomUUID().toString();
        log.info("Scheduling fee payment paid notification for: {}, with task id: {}", feePaymentId, taskId);

        schedulerClient.scheduleIfNotExists(
            FeePaymentPaidNotificationTaskComponent.FEE_PAYMENT_PAID_TASK_DESCRIPTOR
                .instance(taskId)
                .data(FeePaymentStatusChangeTaskData.builder()
                          .feePaymentId(feePaymentId)
                          .build())
                .scheduledTo(Instant.now())
        );
    }

    private FeesAndPayTaskData toFeesAndPayTaskData(FeePaymentEntity feePaymentEntity) {
        String taskData = feePaymentEntity.getTaskData();
        try {
            log.info("Reading taskdata for {} to FeesAndPayTaskData: {}", feePaymentEntity.getId(), taskData);
            return objectMapper.readValue(taskData, FeesAndPayTaskData.class);
        } catch (IOException e) {
            throw new PaymentCallbackException("Unable to process: " + taskData, e);
        }
    }

    private PartyEntity getResponsibleParty(FeesAndPayTaskData feesAndPayTaskData) {
        return partyService.getPartyEntityByEntityId(
            feesAndPayTaskData.getResponsiblePartyId(),
            feesAndPayTaskData.getCaseReference()
        );
    }

}
