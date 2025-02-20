package uk.gov.hmcts.reform.pcs.notify.model;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class SendEmail {

    private TemplateId templateId;
    private String emailAddress;
    private Personalisation personalisation;
    private Reference reference;
    private String emailReplyToId;
}
