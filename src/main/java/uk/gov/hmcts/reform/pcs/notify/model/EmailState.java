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

    public final String id;
    public final String emailAddress;
    public final String templateId;
    public final Map<String, Object> personalisation;
    public final String reference;
    public final String emailReplyToId;
    public final String notificationId;

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

    public EmailState withNotificationId(String notificationId) {
        return new EmailState(
            this.id,
            this.emailAddress,
            this.templateId,
            this.personalisation,
            this.reference,
            this.emailReplyToId,
            notificationId
        );
    }
}
