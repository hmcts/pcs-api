package uk.gov.hmcts.reform.pcs.feesandpay.service;

import java.util.UUID;

/**
 * Receives lifecycle notifications for fee payments made automatically from a PBA account on a bulk claim.
 */
public interface BulkPaymentStatusListener {

    void onPbaPaymentSubmitted(UUID bulkClaimSubmissionId, String serviceRequestReference);

    void onPbaPaymentFailed(UUID bulkClaimSubmissionId, String serviceRequestReference, String reason);

    void onPaymentConfirmed(String serviceRequestReference);

}
