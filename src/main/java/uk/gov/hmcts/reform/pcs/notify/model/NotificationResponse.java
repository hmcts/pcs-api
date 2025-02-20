package uk.gov.hmcts.reform.pcs.notify.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.net.URI;
import java.util.Optional;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationResponse {
    private UUID notificationId;
    private Optional<String> reference;
    private Optional<URI> oneClickUnsubscribeURL;
    private UUID templateId;
    private int templateVersion;
    private String templateUri;
    private String body;
    private String subject;
    private Optional<String> fromEmail;
}
