package uk.gov.hmcts.reform.pcs.bulkclaim.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.pcs.bulkclaim.entity.BulkClaimPaymentStatus;
import uk.gov.hmcts.reform.pcs.bulkclaim.entity.BulkClaimSubmissionEntity;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.feesandpay.model.BulkPbaDetails;
import uk.gov.hmcts.reform.pcs.feesandpay.service.PaymentService;

/**
 * Retries the PBA fee payment of a bulk claim whose earlier payment failed, when the claim is resubmitted.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BulkClaimPaymentRetryService {

    private final BulkClaimPaymentStatusUpdater statusUpdater;
    private final PaymentService paymentService;
    private final BulkClaimCaseStateReader caseStateReader;

    /**
     * Whether the submission is a created case whose fee payment failed and which can still be paid: it must
     * still be awaiting payment and its fee must not already be paid.
     */
    public boolean canRetry(BulkClaimSubmissionEntity submission) {
        if (submission.getPaymentStatus() != BulkClaimPaymentStatus.FAILED
            || submission.getCaseReference() == null
            || submission.getServiceRequestReference() == null) {
            return false;
        }
        boolean awaitingPayment = caseStateReader.stateOf(submission.getCaseReference())
            .filter(State.PENDING_CASE_ISSUED.name()::equals)
            .isPresent();
        return awaitingPayment && !paymentService.isServiceRequestPaid(submission.getServiceRequestReference());
    }

    /**
     * Records the new PBA account against the submission and schedules the payment again. Only one concurrent
     * retry proceeds; the others, and a retry whose task is somehow still scheduled, do nothing.
     *
     * @return whether a payment was scheduled
     */
    public boolean retry(BulkClaimSubmissionEntity submission, String pbaAccountNumber,
                         String customerReference, String organisationName) {
        if (!statusUpdater.restartFailedPayment(submission.getId(), pbaAccountNumber, customerReference)) {
            return false;
        }
        String serviceRequestReference = submission.getServiceRequestReference();
        try {
            return paymentService.rescheduleBulkPbaPayment(
                serviceRequestReference,
                submission.getCaseReference(),
                new BulkPbaDetails(pbaAccountNumber, customerReference, organisationName, submission.getId()));
        } catch (RuntimeException e) {
            log.error("Unable to reschedule the PBA payment for service request {}", serviceRequestReference, e);
            statusUpdater.onPbaPaymentFailed(submission.getId(), serviceRequestReference,
                                             "Payment could not be rescheduled");
            throw e;
        }
    }
}
