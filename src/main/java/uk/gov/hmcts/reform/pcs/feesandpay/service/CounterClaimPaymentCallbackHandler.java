package uk.gov.hmcts.reform.pcs.feesandpay.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.kagkarlsson.scheduler.SchedulerClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.pcs.camunda.CamundaService;
import uk.gov.hmcts.reform.pcs.camunda.TaskType;
import uk.gov.hmcts.reform.pcs.ccd.domain.LanguageUsed;
import uk.gov.hmcts.reform.pcs.ccd.domain.respondpossessionclaim.CounterClaimState;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.DocumentEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.feesandpay.FeePaymentEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.respondpossessionclaim.CounterClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.respondpossessionclaim.DefendantResponseEntity;
import uk.gov.hmcts.reform.pcs.ccd.model.CounterClaimStatusChangeTaskData;
import uk.gov.hmcts.reform.pcs.ccd.repository.CounterClaimRepository;
import uk.gov.hmcts.reform.pcs.ccd.service.counterclaimform.CounterClaimFormScheduler;
import uk.gov.hmcts.reform.pcs.ccd.service.workallocation.TaskDescriptionService;
import uk.gov.hmcts.reform.pcs.ccd.task.CounterClaimIssuedNotificationTaskComponent;
import uk.gov.hmcts.reform.pcs.feesandpay.model.FeesAndPayTaskData;
import uk.gov.hmcts.reform.pcs.feesandpay.model.PaymentStatus;
import uk.gov.hmcts.reform.pcs.feesandpay.model.PaymentStatusCallback;

import java.io.IOException;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Component
@Slf4j
public class CounterClaimPaymentCallbackHandler implements PaymentCallbackStrategy {

    private final CounterClaimRepository counterClaimRepository;
    private final SchedulerClient schedulerClient;
    private final CounterClaimFormScheduler counterClaimFormScheduler;
    private final CamundaService camundaService;
    private final TaskDescriptionService taskDescriptionService;
    private final ObjectMapper objectMapper;
    private final Clock utcClock;

    public CounterClaimPaymentCallbackHandler(CounterClaimRepository counterClaimRepository,
                                              SchedulerClient schedulerClient,
                                              CounterClaimFormScheduler counterClaimFormScheduler,
                                              CamundaService camundaService,
                                              TaskDescriptionService taskDescriptionService,
                                              ObjectMapper objectMapper,
                                              @Qualifier("utcClock") Clock utcClock) {
        this.counterClaimRepository = counterClaimRepository;
        this.schedulerClient = schedulerClient;
        this.counterClaimFormScheduler = counterClaimFormScheduler;
        this.camundaService = camundaService;
        this.taskDescriptionService = taskDescriptionService;
        this.objectMapper = objectMapper;
        this.utcClock = utcClock;
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

        if (PaymentStatus.PAID == feePaymentEntity.getPaymentStatus()) {
            if (counterClaimEntity.getStatus() != CounterClaimState.PENDING_COUNTER_CLAIM_ISSUED) {
                log.warn(
                    "Ignoring paid counterclaim payment callback for counterClaimId {} in status {}",
                    counterClaimId,
                    counterClaimEntity.getStatus()
                );
                return;
            }

            List<DocumentEntity> documentsRequiringTranslation =
                getDocumentsRequiringTranslation(counterClaimEntity);
            counterClaimFormScheduler.scheduleCounterClaimFormGeneration(counterClaimId);

            if (!documentsRequiringTranslation.isEmpty()) {
                counterClaimEntity.setStatus(CounterClaimState.AWAITING_CASEWORKER_REVIEW);
                createTranslateDefendantDocumentTask(counterClaimEntity, documentsRequiringTranslation);
            } else {
                counterClaimEntity.setStatus(CounterClaimState.COUNTER_CLAIM_ISSUED);
                counterClaimEntity.setClaimIssuedDate(LocalDateTime.now(utcClock));
                scheduleCounterClaimIssuedNotification(counterClaimEntity, feePaymentEntity);
            }
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
                .data(CounterClaimStatusChangeTaskData.builder()
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

        LanguageUsed languageUsed = defendantResponse.getLanguageUsed();
        if (languageUsed != LanguageUsed.WELSH && languageUsed != LanguageUsed.ENGLISH_AND_WELSH) {
            return List.of();
        }

        return counterClaimEntity.getPcsCase().getDocuments().stream()
            .filter(document -> !document.isRemoved()
                && document.getCounterClaim() != null
                && document.getCounterClaim().getId().equals(counterClaimEntity.getId()))
            .toList();
    }

    private void createTranslateDefendantDocumentTask(CounterClaimEntity counterClaimEntity,
                                                      List<DocumentEntity> documents) {
        PartyEntity party = counterClaimEntity.getParty();
        PcsCaseEntity pcsCaseEntity = counterClaimEntity.getPcsCase();
        ClaimEntity mainClaim = pcsCaseEntity.getClaims().getFirst();
        long caseReference = pcsCaseEntity.getCaseReference();

        String description = taskDescriptionService.createTranslateDefendantDocumentDescription(
            caseReference, mainClaim, party, documents);

        camundaService.createTask(caseReference, TaskType.TRANSLATE_DEFENDANT_SUBMITTED_DOCUMENT, description);
    }
}
