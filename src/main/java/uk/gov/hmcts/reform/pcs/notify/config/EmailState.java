package uk.gov.hmcts.reform.pcs.notify.config;

import java.io.Serial;
import java.io.Serializable;
import java.util.Map;

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
    public final int retryCount;

    // Default constructor for serialization
    public EmailState() {
        this(null, null, null, null, null, null,
            null, 0);
    }

    public EmailState(String id, String emailAddress, String templateId,
                      Map<String, Object> personalisation, String reference,
                      String emailReplyToId, String notificationId, int retryCount) {
        this.id = id;
        this.emailAddress = emailAddress;
        this.templateId = templateId;
        this.personalisation = personalisation;
        this.reference = reference;
        this.emailReplyToId = emailReplyToId;
        this.notificationId = notificationId;
        this.retryCount = retryCount;
    }

    @Override
    public String toString() {
        return "EmailState{"
            + "id='" + id + '\''
            + ", emailAddress='" + emailAddress + '\''
            + ", templateId='" + templateId + '\''
            + ", notificationId='" + notificationId + '\''
            + ", retryCount=" + retryCount
            + '}';
    }
}
