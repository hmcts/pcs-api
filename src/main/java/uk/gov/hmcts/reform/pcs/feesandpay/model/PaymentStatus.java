package uk.gov.hmcts.reform.pcs.feesandpay.model;

import lombok.Getter;

@Getter
public enum PaymentStatus {

    PAID("Paid"),
    NOT_PAID("Not paid"),
    PARTIALLY_PAID("Partially paid");

    private final String value;

    public static PaymentStatus fromValue(String value) {
        for (PaymentStatus status : values()) {
            if (status.value.equalsIgnoreCase(value)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown PaymentStatus value: " + value);
    }

    PaymentStatus(String value) {
        this.value = value;
    }
}
