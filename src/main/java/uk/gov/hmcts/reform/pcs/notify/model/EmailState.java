package uk.gov.hmcts.reform.pcs.notify.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.util.Map;
import java.util.UUID;

@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class EmailState implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private String id;
    private String emailAddress;
    private String templateId;
    private Map<String, Object> personalisation;
    private String reference;
    private String emailReplyToId;
    private String notificationId; // GOV.UK Notify notification ID (set after sending)
    private UUID dbNotificationId; // Database notification record ID (set before sending)
}
