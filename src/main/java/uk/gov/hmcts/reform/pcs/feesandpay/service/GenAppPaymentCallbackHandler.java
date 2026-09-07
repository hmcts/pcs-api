package uk.gov.hmcts.reform.pcs.feesandpay.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.SystemEventExecutionResult;
import uk.gov.hmcts.ccd.sdk.SystemEventExecutor;
import uk.gov.hmcts.ccd.sdk.SystemEventResult;
import uk.gov.hmcts.reform.pcs.ccd.domain.genapp.GenAppState;
import uk.gov.hmcts.reform.pcs.ccd.entity.GenAppEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.feesandpay.FeePaymentEntity;
import uk.gov.hmcts.reform.pcs.ccd.event.genapp.GenAppWaTaskService;
import uk.gov.hmcts.reform.pcs.ccd.repository.GenAppRepository;
import uk.gov.hmcts.reform.pcs.ccd.repository.feeandpay.FeePaymentRepository;
import uk.gov.hmcts.reform.pcs.ccd.service.genapp.GenAppDocumentGenerator;
import uk.gov.hmcts.reform.pcs.exception.GenAppNotFoundException;
import uk.gov.hmcts.reform.pcs.feesandpay.model.PaymentStatus;
import uk.gov.hmcts.reform.pcs.feesandpay.model.PaymentStatusCallback;
import uk.gov.hmcts.reform.pcs.notify.service.NotificationService;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import static uk.gov.hmcts.reform.pcs.ccd.event.EventId.genAppIssued;

/**
 * Handles the general-application fee's paid callback by recording a {@code genAppIssued} system
 * event: the fee update and the gen-app state transition commit in one transaction with the case
 * snapshot and history entry. The document generation, notification and work-allocation side
 * effects run after commit (only on first execution), keeping HTTP work out of the transaction.
 */
@Component
@Slf4j
public class GenAppPaymentCallbackHandler implements PaymentCallbackStrategy {

    private static final String EVENT_NAME = "General application issued";

    private final SystemEventExecutor systemEventExecutor;
    private final GenAppRepository genAppRepository;
    private final FeePaymentRepository feePaymentRepository;
    private final GenAppDocumentGenerator genAppDocumentGenerator;
    private final NotificationService notificationService;
    private final GenAppWaTaskService genAppWaTaskService;

    public GenAppPaymentCallbackHandler(@Lazy SystemEventExecutor systemEventExecutor,
                                        GenAppRepository genAppRepository,
                                        FeePaymentRepository feePaymentRepository,
                                        GenAppDocumentGenerator genAppDocumentGenerator,
                                        NotificationService notificationService,
                                        GenAppWaTaskService genAppWaTaskService) {
        this.systemEventExecutor = systemEventExecutor;
        this.genAppRepository = genAppRepository;
        this.feePaymentRepository = feePaymentRepository;
        this.genAppDocumentGenerator = genAppDocumentGenerator;
        this.notificationService = notificationService;
        this.genAppWaTaskService = genAppWaTaskService;
    }

    @Override
    public boolean handlesOwnTransaction(PaymentStatusCallback paymentStatusCallback) {
        return isPaid(paymentStatusCallback);
    }

    @Override
    public void handle(PaymentStatusCallback paymentStatusCallback, FeePaymentEntity feePaymentEntity) {
        UUID genAppId = feePaymentEntity.getRelatedEntityId();
        log.info("Handling callback for gen app payment for gen app ID {}", genAppId);

        if (!isPaid(paymentStatusCallback)) {
            log.warn("The payment was not successful [{}] for gen app {} on case {}",
                     paymentStatusCallback.getServiceRequestStatus(), genAppId,
                     paymentStatusCallback.getCcdCaseNumber());
            return;
        }

        GenAppEntity genAppEntity = findGenAppEntity(genAppId);
        if (genAppEntity.getState() != GenAppState.PENDING_GEN_APP_ISSUED) {
            log.warn("Gen app {} state {} not valid for this callback", genAppId, genAppEntity.getState());
            return;
        }

        long caseReference = genAppEntity.getPcsCase().getCaseReference();

        SystemEventExecutionResult result = systemEventExecutor.execute(
            caseReference, idempotencyKey(feePaymentEntity), context -> {
                feePaymentEntity.setExternalReference(paymentStatusCallback.getPaymentReference());
                feePaymentEntity.setPaymentStatus(PaymentStatus.PAID);
                feePaymentRepository.save(feePaymentEntity);

                genAppEntity.setState(GenAppState.GEN_APP_ISSUED);
                genAppRepository.save(genAppEntity);

                return SystemEventResult.withoutStateTransition(genAppIssued.name(), EVENT_NAME);
            });

        if (result.outcome() == SystemEventExecutionResult.Outcome.EXECUTED) {
            genAppDocumentGenerator.createSubmissionDocument(caseReference, genAppEntity);
            notificationService.sendGenAppReceivedEmail(genAppEntity);
            genAppWaTaskService.createReviewGenAppTask(caseReference, genAppEntity);
            genAppWaTaskService.createTranslationTaskForGenApp(genAppEntity);
        }
    }

    private boolean isPaid(PaymentStatusCallback paymentStatusCallback) {
        return PaymentStatus.PAID == PaymentStatus.fromValue(paymentStatusCallback.getServiceRequestStatus());
    }

    private UUID idempotencyKey(FeePaymentEntity feePaymentEntity) {
        String key = genAppIssued.name() + ":" + feePaymentEntity.getServiceRequestReference();
        return UUID.nameUUIDFromBytes(key.getBytes(StandardCharsets.UTF_8));
    }

    private GenAppEntity findGenAppEntity(UUID genAppId) {
        return genAppRepository.findById(genAppId)
            .orElseThrow(() -> new GenAppNotFoundException("Unable to find gen app with ID " + genAppId));
    }

}
