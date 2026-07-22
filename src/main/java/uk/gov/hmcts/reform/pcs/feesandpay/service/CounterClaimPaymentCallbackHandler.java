package uk.gov.hmcts.reform.pcs.feesandpay.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.kagkarlsson.scheduler.SchedulerClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.pcs.ccd.domain.respondpossessionclaim.CounterClaimState;
import uk.gov.hmcts.reform.pcs.ccd.entity.feesandpay.FeePaymentEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.respondpossessionclaim.CounterClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.model.CounterClaimStatusChangeTaskData;
import uk.gov.hmcts.reform.pcs.ccd.repository.CounterClaimRepository;
import uk.gov.hmcts.reform.pcs.ccd.service.counterclaimform.CounterClaimFormScheduler;
import uk.gov.hmcts.reform.pcs.ccd.task.CounterClaimIssuedNotificationTaskComponent;
import uk.gov.hmcts.reform.pcs.exception.RedactionContext;
import uk.gov.hmcts.reform.pcs.feesandpay.model.FeesAndPayTaskData;
import uk.gov.hmcts.reform.pcs.feesandpay.model.PaymentStatus;
import uk.gov.hmcts.reform.pcs.feesandpay.model.PaymentStatusCallback;

import java.io.IOException;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;

import static uk.gov.hmcts.reform.pcs.exception.ErrorCode.COUNTER_CLAIM_CALLBACK;
import static uk.gov.hmcts.reform.pcs.exception.ErrorCode.COUNTER_CLAIM_TASK_DATA;

@Component
@Slf4j
public class CounterClaimPaymentCallbackHandler implements PaymentCallbackStrategy {

    private final CounterClaimRepository counterClaimRepository;
    private final SchedulerClient schedulerClient;
    private final CounterClaimFormScheduler counterClaimFormScheduler;
    private final ObjectMapper objectMapper;
    private final Clock utcClock;

    public CounterClaimPaymentCallbackHandler(CounterClaimRepository counterClaimRepository,
                                              SchedulerClient schedulerClient,
                                              CounterClaimFormScheduler counterClaimFormScheduler,
                                              ObjectMapper objectMapper,
                                              @Qualifier("utcClock") Clock utcClock) {
        this.counterClaimRepository = counterClaimRepository;
        this.schedulerClient = schedulerClient;
        this.counterClaimFormScheduler = counterClaimFormScheduler;
        this.objectMapper = objectMapper;
        this.utcClock = utcClock;
    }

    @Override
    public void handle(PaymentStatusCallback paymentStatusCallback, FeePaymentEntity feePaymentEntity) {
        FeesAndPayTaskData feesAndPayTaskData = toFeesAndPayTaskData(feePaymentEntity.getTaskData());
        UUID counterClaimId = feesAndPayTaskData.getRelatedEntityId();
        if (counterClaimId == null) {
            throw new PaymentCallbackException(COUNTER_CLAIM_CALLBACK);
        }

        CounterClaimEntity counterClaimEntity = counterClaimRepository.findById(counterClaimId)
            .orElseThrow(() -> new IllegalArgumentException("Counterclaim not found: " + counterClaimId));

        if (PaymentStatus.PAID == feePaymentEntity.getPaymentStatus()) {
            if (counterClaimEntity.getStatus() != CounterClaimState.PENDING_COUNTER_CLAIM_ISSUED) {
                log.warn(
                    "Ignoring paid counterclaim payment callback for counterClaimId {} in status {}",
                    counterClaimId,
                    counterClaimEntity.getStatus()
                );
                return;
            }

            counterClaimEntity.setStatus(CounterClaimState.COUNTER_CLAIM_ISSUED);
            counterClaimEntity.setClaimIssuedDate(LocalDateTime.now(utcClock));
            scheduleCounterClaimIssuedNotification(counterClaimEntity, feePaymentEntity);
            counterClaimFormScheduler.scheduleCounterClaimFormGeneration(counterClaimId);
            return;
        }

        log.warn(
            "Counterclaim payment unsuccessful [{}] for counterClaimId {}",
            feePaymentEntity.getPaymentStatus(),
            counterClaimId
        );
    }

    private FeesAndPayTaskData toFeesAndPayTaskData(String feesAndPayTaskDataAsString) {
        try {
            return objectMapper.readValue(feesAndPayTaskDataAsString, FeesAndPayTaskData.class);
        } catch (IOException e) {
            throw new PaymentCallbackException(COUNTER_CLAIM_TASK_DATA,
                RedactionContext.of("Unable to process: ", feesAndPayTaskDataAsString), e);
        }
    }

    private void scheduleCounterClaimIssuedNotification(CounterClaimEntity counterClaimEntity,
                                                        FeePaymentEntity feePaymentEntity) {

        String taskId = UUID.randomUUID().toString();
        UUID counterClaimId = counterClaimEntity.getId();
        log.info("Scheduling counter claim issued notification for: {}, with task id: {}", counterClaimId, taskId);

        schedulerClient.scheduleIfNotExists(
            CounterClaimIssuedNotificationTaskComponent.COUNTER_CLAIM_ISSUED_TASK_DESCRIPTOR
                .instance(taskId)
                .data(CounterClaimStatusChangeTaskData.builder()
                          .counterClaimId(counterClaimId)
                          .paymentReference(feePaymentEntity.getExternalReference())
                          .build())
                .scheduledTo(Instant.now())
        );
    }
}
