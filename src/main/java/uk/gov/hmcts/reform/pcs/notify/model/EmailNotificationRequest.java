package uk.gov.hmcts.reform.pcs.notify.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class EmailNotificationRequest {

    private String templateId;
    private String emailAddress;
    private Map<String, Object> personalisation;
    private String reference;
    private String emailReplyToId;
}
