package uk.gov.hmcts.reform.pcs.notify.model;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class SendTextMessage {

    private TemplateId templateId;
    private String phoneNumber;
    private Personalisation personalisation;
    private Reference reference;
    private String smsSenderId;
}
