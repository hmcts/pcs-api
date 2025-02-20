package uk.gov.hmcts.reform.pcs.notify.model;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class GetMultipleMessagesStatus {

    private String status;
    private String notificationType;
    private String reference;
    private String olderThanId;
}
