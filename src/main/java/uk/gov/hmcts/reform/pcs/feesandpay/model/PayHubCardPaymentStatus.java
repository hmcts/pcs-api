package uk.gov.hmcts.reform.pcs.feesandpay.model;

import java.util.Locale;
import java.util.Set;

public final class PayHubCardPaymentStatus {

    private static final Set<String> SUCCESS_STATUSES = Set.of("success", "paid");

    private PayHubCardPaymentStatus() {
    }

    public static boolean isSuccessful(String status) {
        if (status == null || status.isBlank()) {
            return false;
        }
        return SUCCESS_STATUSES.contains(status.trim().toLowerCase(Locale.ROOT));
    }
}
