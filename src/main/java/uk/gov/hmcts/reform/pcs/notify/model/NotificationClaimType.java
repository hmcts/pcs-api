package uk.gov.hmcts.reform.pcs.notify.model;

import lombok.Getter;

@Getter
public enum NotificationClaimType {
    COUNTER_CLAIM("counter_claim"),
    NO_COUNTER_CLAIM("no_counter_claim"),
    POSSESSION_CLAIM("possession_claim"),
    GENERAL_APPLICATION_CLAIM("general_application_claim");

    private final String value;

    NotificationClaimType(String value) {
        this.value = value;
    }

    public static NotificationClaimType fromString(String status) {
        for (NotificationClaimType notificationClaimType : NotificationClaimType.values()) {
            if (notificationClaimType.value.equalsIgnoreCase(status)) {
                return notificationClaimType;
            }
        }
        throw new IllegalArgumentException("Unknown status: " + status);
    }
}
