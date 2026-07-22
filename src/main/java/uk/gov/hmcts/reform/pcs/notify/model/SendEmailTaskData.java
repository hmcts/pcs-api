package uk.gov.hmcts.reform.pcs.notify.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.util.Map;

@Data
@Builder(toBuilder = true)
@AllArgsConstructor
public class SendEmailTaskData {

    private final String id;
    private final String emailAddress;
    private final String templateId;
    private final Map<String, Object> personalisation;
    private final String reference;
    private final String emailReplyToId;
    private final String notificationId; // GOV.UK Notify notification ID (set after sending)
    private final Integer dbNotificationId; // Database notification record ID (set before sending)

}
