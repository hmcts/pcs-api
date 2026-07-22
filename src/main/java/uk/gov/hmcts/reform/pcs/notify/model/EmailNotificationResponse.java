package uk.gov.hmcts.reform.pcs.notify.model;

import lombok.Data;

@Data
public class EmailNotificationResponse {
    private String taskId;
    private String status;
    private Integer notificationId; // Database notification record ID
}
