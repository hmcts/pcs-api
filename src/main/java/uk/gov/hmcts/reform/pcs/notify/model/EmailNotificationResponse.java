package uk.gov.hmcts.reform.pcs.notify.model;

import lombok.Data;

@Data
public class EmailNotificationResponse {
    private String taskId;
    private String status;
    private Long notificationId; // Database notification record ID
}
