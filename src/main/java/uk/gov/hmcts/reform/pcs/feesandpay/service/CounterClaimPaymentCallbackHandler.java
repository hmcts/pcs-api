package uk.gov.hmcts.reform.pcs.feesandpay.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.kagkarlsson.scheduler.SchedulerClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.SystemEventExecutionResult;
import uk.gov.hmcts.ccd.sdk.SystemEventExecutor;
import uk.gov.hmcts.ccd.sdk.SystemEventResult;
import uk.gov.hmcts.reform.pcs.ccd.domain.DocumentType;
import uk.gov.hmcts.reform.pcs.ccd.domain.respondpossessionclaim.CounterClaimState;
import uk.gov.hmcts.reform.pcs.ccd.entity.DocumentEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.feesandpay.FeePaymentEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.respondpossessionclaim.CounterClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.respondpossessionclaim.DefendantResponseEntity;
import uk.gov.hmcts.reform.pcs.ccd.model.CounterClaimTaskData;
import uk.gov.hmcts.reform.pcs.ccd.repository.CounterClaimRepository;
import uk.gov.hmcts.reform.pcs.ccd.repository.feeandpay.FeePaymentRepository;
import uk.gov.hmcts.reform.pcs.ccd.service.counterclaimform.CounterClaimFormScheduler;
import uk.gov.hmcts.reform.pcs.ccd.service.workallocation.TranslationWAService;
import uk.gov.hmcts.reform.pcs.ccd.task.CounterClaimIssuedNotificationTaskComponent;
import uk.gov.hmcts.reform.pcs.feesandpay.model.FeesAndPayTaskData;
import uk.gov.hmcts.reform.pcs.feesandpay.model.PaymentStatus;
import uk.gov.hmcts.reform.pcs.feesandpay.model.PaymentStatusCallback;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static uk.gov.hmcts.reform.pcs.ccd.event.EventId.counterClaimIssued;

/**
 * Handles the counterclaim fee's paid callback by recording a {@code counterClaimIssued} system
 * event: the fee update and the counterclaim status transition commit in one transaction with the
 * case snapshot and history entry. The notification, form-generation and translation side effects
 * run after commit (only on first execution), so no external work sits inside the event transaction.
 */
@Component
@Slf4j
public class CounterClaimPaymentCallbackHandler implements PaymentCallbackStrategy {

    private static final String EVENT_NAME = "Counterclaim issued";

    private final SystemEventExecutor systemEventExecutor;
    private final CounterClaimRepository counterClaimRepository;
    private final FeePaymentRepository feePaymentRepository;
    private final SchedulerClient schedulerClient;
    private final CounterClaimFormScheduler counterClaimFormScheduler;
    private final TranslationWAService translationWAService;
    private final ObjectMapper objectMapper;
    private final Clock utcClock;

    public CounterClaimPaymentCallbackHandler(@Lazy SystemEventExecutor systemEventExecutor,
                                              CounterClaimRepository counterClaimRepository,
                                              FeePaymentRepository feePaymentRepository,
                                              SchedulerClient schedulerClient,
                                              CounterClaimFormScheduler counterClaimFormScheduler,
                                              TranslationWAService translationWAService,
                                              ObjectMapper objectMapper,
                                              @Qualifier("utcClock") Clock utcClock) {
        this.systemEventExecutor = systemEventExecutor;
        this.counterClaimRepository = counterClaimRepository;
        this.feePaymentRepository = feePaymentRepository;
        this.schedulerClient = schedulerClient;
        this.counterClaimFormScheduler = counterClaimFormScheduler;
        this.translationWAService = translationWAService;
        this.objectMapper = objectMapper;
        this.utcClock = utcClock;
    }

    @Override
    public boolean handlesOwnTransaction(PaymentStatusCallback paymentStatusCallback) {
        return isPaid(paymentStatusCallback);
    }

    @Override
    public void handle(PaymentStatusCallback paymentStatusCallback, FeePaymentEntity feePaymentEntity) {
        FeesAndPayTaskData feesAndPayTaskData = toFeesAndPayTaskData(feePaymentEntity.getTaskData());
        UUID counterClaimId = feesAndPayTaskData.getRelatedEntityId();
        if (counterClaimId == null) {
            throw new PaymentCallbackException(
                "Counterclaim payment callback missing relatedEntityId in task data",
                null
            );
        }

        CounterClaimEntity counterClaimEntity = counterClaimRepository.findById(counterClaimId)
            .orElseThrow(() -> new IllegalArgumentException("Counterclaim not found: " + counterClaimId));

        if (!isPaid(paymentStatusCallback)) {
            log.warn("Counterclaim payment unsuccessful [{}] for counterClaimId {}",
                     paymentStatusCallback.getServiceRequestStatus(), counterClaimId);
            return;
        }

        if (counterClaimEntity.getStatus() != CounterClaimState.PENDING_COUNTER_CLAIM_ISSUED) {
            log.warn("Ignoring paid counterclaim payment callback for counterClaimId {} in status {}",
                     counterClaimId, counterClaimEntity.getStatus());
            return;
        }

        long caseReference = counterClaimEntity.getPcsCase().getCaseReference();

        SystemEventExecutionResult result = systemEventExecutor.execute(
            caseReference, idempotencyKey(feePaymentEntity), context -> {
                feePaymentEntity.setExternalReference(paymentStatusCallback.getPaymentReference());
                feePaymentEntity.setPaymentStatus(PaymentStatus.PAID);
                feePaymentRepository.save(feePaymentEntity);

                counterClaimEntity.setStatus(CounterClaimState.COUNTER_CLAIM_ISSUED);
                counterClaimEntity.setClaimIssuedDate(LocalDateTime.now(utcClock));
                counterClaimRepository.save(counterClaimEntity);

                return SystemEventResult.withoutStateTransition(counterClaimIssued.name(), EVENT_NAME);
            });

        if (result.outcome() == SystemEventExecutionResult.Outcome.EXECUTED) {
            scheduleCounterClaimIssuedNotification(counterClaimEntity, feePaymentEntity);
            counterClaimFormScheduler.scheduleCounterClaimFormGeneration(counterClaimId);
            translationWAService.createTranslateDefendantSubmittedDocumentTask(
                counterClaimEntity.getPcsCase(), counterClaimEntity.getParty(),
                getDocumentsRequiringTranslation(counterClaimEntity));
        }
    }

    private boolean isPaid(PaymentStatusCallback paymentStatusCallback) {
        return PaymentStatus.PAID == PaymentStatus.fromValue(paymentStatusCallback.getServiceRequestStatus());
    }

    private UUID idempotencyKey(FeePaymentEntity feePaymentEntity) {
        String key = counterClaimIssued.name() + ":" + feePaymentEntity.getServiceRequestReference();
        return UUID.nameUUIDFromBytes(key.getBytes(StandardCharsets.UTF_8));
    }

    private FeesAndPayTaskData toFeesAndPayTaskData(String feesAndPayTaskDataAsString) {
        try {
            return objectMapper.readValue(feesAndPayTaskDataAsString, FeesAndPayTaskData.class);
        } catch (IOException e) {
            throw new PaymentCallbackException("Unable to process: " + feesAndPayTaskDataAsString, e);
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
                .data(CounterClaimTaskData.builder()
                          .counterClaimId(counterClaimId)
                          .paymentReference(feePaymentEntity.getExternalReference())
                          .build())
                .scheduledTo(Instant.now())
        );
    }

    private List<DocumentEntity> getDocumentsRequiringTranslation(CounterClaimEntity counterClaimEntity) {
        DefendantResponseEntity defendantResponse = counterClaimEntity.findAssociatedDefendantResponse()
            .orElse(null);

        if (defendantResponse == null) {
            return List.of();
        }

        if (!translationWAService.isTranslationRequired(defendantResponse.getLanguageUsed())) {
            return List.of();
        }

        return counterClaimEntity.getPcsCase().getDocuments().stream()
            .filter(document -> !document.isRemoved()
                && document.getType() != DocumentType.COUNTERCLAIM
                && document.getCounterClaim() != null
                && document.getCounterClaim().getId().equals(counterClaimEntity.getId()))
            .toList();
    }
}
