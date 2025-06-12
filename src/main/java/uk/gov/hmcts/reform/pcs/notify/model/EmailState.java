package uk.gov.hmcts.reform.pcs.notify.model;

import lombok.Builder;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Map;

@Data
@Builder(toBuilder = true)
public class EmailState implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private final String id;
    private final String emailAddress;
    private final String templateId;
    private final Map<String, Object> personalisation;
    private final String reference;
    private final String emailReplyToId;
    private final String notificationId;

    // Default constructor for serialization
    public EmailState() {
        this(null, null, null, null, null, null,
            null);
    }

    public EmailState(String id, String emailAddress, String templateId,
                      Map<String, Object> personalisation, String reference,
                      String emailReplyToId, String notificationId) {
        this.id = id;
        this.emailAddress = emailAddress;
        this.templateId = templateId;
        this.personalisation = personalisation;
        this.reference = reference;
        this.emailReplyToId = emailReplyToId;
        this.notificationId = notificationId;
    }
}
