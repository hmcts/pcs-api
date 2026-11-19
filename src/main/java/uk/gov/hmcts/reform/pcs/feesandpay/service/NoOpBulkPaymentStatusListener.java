package uk.gov.hmcts.reform.pcs.feesandpay.service;

import java.util.UUID;

public class NoOpBulkPaymentStatusListener implements BulkPaymentStatusListener {

    @Override
    public void onPbaPaymentSubmitted(UUID bulkClaimSubmissionId, String serviceRequestReference) {
        // intentionally empty
    }

    @Override
    public void onPbaPaymentFailed(UUID bulkClaimSubmissionId, String serviceRequestReference, String reason) {
        // intentionally empty
    }

    @Override
    public void onPaymentConfirmed(String serviceRequestReference) {
        // intentionally empty
    }

}
