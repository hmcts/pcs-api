package uk.gov.hmcts.reform.pcs.notify.model;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
public class SendTextMessage {

    private String templateId;
    private String phoneNumber;
    private Map<String, Object> personalisation;
    private String reference;
    private String smsSenderId;
}
