package uk.gov.hmcts.reform.pcs.bulkclaim.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.reform.pcs.bulkclaim.entity.BulkClaimPaymentStatus;
import uk.gov.hmcts.reform.pcs.bulkclaim.entity.BulkClaimSubmissionEntity;
import uk.gov.hmcts.reform.pcs.bulkclaim.repository.BulkClaimSubmissionRepository;
import uk.gov.hmcts.reform.pcs.feesandpay.service.BulkPaymentStatusListener;

import java.util.Optional;
import java.util.UUID;

/**
 * Keeps the payment state of a bulk claim submission in step with its fee payment from the PBA account.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BulkClaimPaymentStatusUpdater implements BulkPaymentStatusListener {

    private final BulkClaimSubmissionRepository repository;

    @Override
    @Transactional
    public void onPbaPaymentSubmitted(UUID bulkClaimSubmissionId, String serviceRequestReference) {
        findById(bulkClaimSubmissionId).ifPresent(submission -> {
            submission.setServiceRequestReference(serviceRequestReference);
            submission.setPaymentStatus(BulkClaimPaymentStatus.PENDING);
            submission.setPaymentFailureReason(null);
            repository.save(submission);
        });
    }

    @Override
    @Transactional
    public void onPbaPaymentFailed(UUID bulkClaimSubmissionId, String serviceRequestReference, String reason) {
        findById(bulkClaimSubmissionId).ifPresent(submission -> {
            submission.setServiceRequestReference(serviceRequestReference);
            submission.setPaymentStatus(BulkClaimPaymentStatus.FAILED);
            submission.setPaymentFailureReason(reason);
            repository.save(submission);
        });
    }

    @Override
    @Transactional
    public void onPaymentConfirmed(String serviceRequestReference) {
        repository.findByServiceRequestReference(serviceRequestReference).ifPresent(submission -> {
            submission.setPaymentStatus(BulkClaimPaymentStatus.PAID);
            submission.setPaymentFailureReason(null);
            repository.save(submission);
        });
    }

    private Optional<BulkClaimSubmissionEntity> findById(UUID bulkClaimSubmissionId) {
        if (bulkClaimSubmissionId == null) {
            return Optional.empty();
        }
        Optional<BulkClaimSubmissionEntity> submission = repository.findById(bulkClaimSubmissionId);
        if (submission.isEmpty()) {
            log.warn("No bulk claim submission {} to record a payment status against", bulkClaimSubmissionId);
        }
        return submission;
    }
}
