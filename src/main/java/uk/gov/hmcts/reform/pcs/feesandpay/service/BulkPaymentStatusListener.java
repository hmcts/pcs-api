package uk.gov.hmcts.reform.pcs.feesandpay.service;

import java.util.UUID;

/**
 * Receives lifecycle notifications for fee payments made automatically from a PBA account on a bulk claim.
 */
public interface BulkPaymentStatusListener {

    /**
     * Called in the transaction that records the service request, so the submission can be found by its service
     * request reference from the moment a payment callback could arrive.
     */
    void onServiceRequestCreated(UUID bulkClaimSubmissionId, String serviceRequestReference);

    void onPbaPaymentSubmitted(UUID bulkClaimSubmissionId, String serviceRequestReference);

    void onPbaPaymentFailed(UUID bulkClaimSubmissionId, String serviceRequestReference, String reason);

    /**
     * Runs after the payment callback has committed and never affects it; a failure here is logged and swallowed.
     */
    void onPaymentConfirmed(String serviceRequestReference);

}
