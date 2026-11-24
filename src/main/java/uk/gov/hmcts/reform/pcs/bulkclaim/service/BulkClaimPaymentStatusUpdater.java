package uk.gov.hmcts.reform.pcs.bulkclaim.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;
import uk.gov.hmcts.reform.pcs.bulkclaim.entity.BulkClaimPaymentStatus;
import uk.gov.hmcts.reform.pcs.bulkclaim.entity.BulkClaimSubmissionEntity;
import uk.gov.hmcts.reform.pcs.bulkclaim.repository.BulkClaimSubmissionRepository;
import uk.gov.hmcts.reform.pcs.feesandpay.service.BulkPaymentStatusListener;

import java.util.Optional;
import java.util.UUID;

/**
 * Keeps the payment state of a bulk claim submission in step with its fee payment from the PBA account.
 * PAID is terminal: no later notification moves a submission out of it, whatever order they arrive in.
 */
@Slf4j
@Component
public class BulkClaimPaymentStatusUpdater implements BulkPaymentStatusListener {

    private final BulkClaimSubmissionRepository repository;
    private final TransactionTemplate newTransaction;

    public BulkClaimPaymentStatusUpdater(BulkClaimSubmissionRepository repository,
                                         PlatformTransactionManager transactionManager) {
        this.repository = repository;
        this.newTransaction = new TransactionTemplate(transactionManager);
        this.newTransaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    @Override
    @Transactional
    public void onServiceRequestCreated(UUID bulkClaimSubmissionId, String serviceRequestReference) {
        findById(bulkClaimSubmissionId).ifPresent(submission -> {
            submission.setServiceRequestReference(serviceRequestReference);
            advance(submission, BulkClaimPaymentStatus.PENDING, null);
            repository.save(submission);
        });
    }

    @Override
    @Transactional
    public void onPbaPaymentSubmitted(UUID bulkClaimSubmissionId, String serviceRequestReference) {
        findById(bulkClaimSubmissionId).ifPresent(submission -> {
            submission.setServiceRequestReference(serviceRequestReference);
            advance(submission, BulkClaimPaymentStatus.PENDING, null);
            repository.save(submission);
        });
    }

    @Override
    @Transactional
    public void onPbaPaymentFailed(UUID bulkClaimSubmissionId, String serviceRequestReference, String reason) {
        findById(bulkClaimSubmissionId).ifPresent(submission -> {
            submission.setServiceRequestReference(serviceRequestReference);
            advance(submission, BulkClaimPaymentStatus.FAILED, reason);
            repository.save(submission);
        });
    }

    /**
     * Moves a FAILED payment back to PENDING for an explicit retry with a new PBA account. This is the only way out
     * of FAILED; ordinary notifications never leave it.
     *
     * @return false when the submission has no case, or its payment is not FAILED (for example because a
     *     concurrent retry already restarted it)
     */
    public boolean restartFailedPayment(UUID bulkClaimSubmissionId, String pbaAccountNumber, String customerReference) {
        return Boolean.TRUE.equals(newTransaction.execute(status ->
            repository.findByIdForUpdate(bulkClaimSubmissionId)
                .filter(submission -> submission.getCaseReference() != null
                    && submission.getPaymentStatus() == BulkClaimPaymentStatus.FAILED)
                .map(submission -> {
                    submission.setPaymentStatus(BulkClaimPaymentStatus.PENDING);
                    submission.setPaymentFailureReason(null);
                    submission.setPbaAccountNumber(pbaAccountNumber);
                    submission.setPaymentCustomerReference(customerReference);
                    repository.save(submission);
                    return true;
                })
                .orElse(false)));
    }

    @Override
    public void onPaymentConfirmed(String serviceRequestReference) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    markPaid(serviceRequestReference);
                }
            });
        } else {
            markPaid(serviceRequestReference);
        }
    }

    private void markPaid(String serviceRequestReference) {
        try {
            newTransaction.executeWithoutResult(status ->
                repository.findByServiceRequestReferenceForUpdate(serviceRequestReference).ifPresent(submission -> {
                    advance(submission, BulkClaimPaymentStatus.PAID, null);
                    repository.save(submission);
                }));
        } catch (RuntimeException e) {
            log.error("Unable to mark bulk claim submission paid for service request {}",
                      serviceRequestReference, e);
        }
    }

    private static void advance(BulkClaimSubmissionEntity submission, BulkClaimPaymentStatus target, String reason) {
        BulkClaimPaymentStatus current = submission.getPaymentStatus();
        boolean allowed = switch (target) {
            case PAID -> true;
            case PENDING -> current == null || current == BulkClaimPaymentStatus.NOT_STARTED
                || current == BulkClaimPaymentStatus.PENDING;
            case FAILED -> current != BulkClaimPaymentStatus.PAID;
            case NOT_STARTED -> false;
        };
        if (!allowed) {
            log.info("Ignoring {} payment status for bulk claim submission {} already {}",
                     target, submission.getId(), current);
            return;
        }
        submission.setPaymentStatus(target);
        submission.setPaymentFailureReason(reason);
    }

    private Optional<BulkClaimSubmissionEntity> findById(UUID bulkClaimSubmissionId) {
        if (bulkClaimSubmissionId == null) {
            return Optional.empty();
        }
        Optional<BulkClaimSubmissionEntity> submission = repository.findByIdForUpdate(bulkClaimSubmissionId);
        if (submission.isEmpty()) {
            log.warn("No bulk claim submission {} to record a payment status against", bulkClaimSubmissionId);
        }
        return submission;
    }
}
