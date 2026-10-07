package uk.gov.hmcts.reform.pcs.notification;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

import java.util.Map;

@Data
@Builder
public class CapturedNotification {

    @JsonProperty("template_id")
    private final String templateId;

    @JsonProperty("email_address")
    private final String emailAddress;

    @JsonProperty("personalisation")
    private final Map<String, Object> personalisation;

    @JsonProperty("reference")
    private final String reference;

}
