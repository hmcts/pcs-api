package uk.gov.hmcts.reform.pcs.notify.model;

public enum NotificationStatus {
    CREATED("created"),
    SENDING("sending"),
    DELIVERED("delivered"),
    PERMANENT_FAILURE("permanent-failure"),
    TEMPORARY_FAILURE("temporary-failure"),
    TECHNICAL_FAILURE("technical-failure"),
    SCHEDULE("scheduled"),
    PENDING_SCHEDULE("pending-schedule"), 
    SUBMITTED("submitted");

    private final String value;

    NotificationStatus(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    @Override
    public String toString() {
        return value;
    }

    public static NotificationStatus fromString(String status) {
        for (NotificationStatus notificationStatus : NotificationStatus.values()) {
            if (notificationStatus.value.equalsIgnoreCase(status)) {
                return notificationStatus;
            }
        }
        throw new IllegalArgumentException("Unknown status: " + status);
    }
}
