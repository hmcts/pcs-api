package uk.gov.hmcts.reform.pcs.notify.model;

import lombok.Getter;

@Getter
public enum NotificationClaimType {
    COUNTER_CLAIM("counter_claim"),
    NO_COUNTER_CLAIM("no_counter_claim"),
    POSSESSION_CLAIM("possession_claim"),
    GENERAL_APPLICATION("general_application");

    private final String value;

    NotificationClaimType(String value) {
        this.value = value;
    }

}
