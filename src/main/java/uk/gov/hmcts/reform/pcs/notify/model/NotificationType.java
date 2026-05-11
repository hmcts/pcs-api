package uk.gov.hmcts.reform.pcs.notify.model;

import lombok.RequiredArgsConstructor;

import java.util.Arrays;

@RequiredArgsConstructor
public enum NotificationType {
    EMAIL("Email");

    private final String value;

    @Override
    public String toString() {
        return value;
    }

    public static NotificationType fromString(String status) {
        return Arrays.stream(values())
            .filter(s -> s.value.equalsIgnoreCase(status))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Unknown status: " + status));
    }
}
