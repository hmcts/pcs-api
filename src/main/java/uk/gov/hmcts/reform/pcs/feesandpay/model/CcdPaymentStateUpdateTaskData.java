package uk.gov.hmcts.reform.pcs.feesandpay.model;

import lombok.Builder;
import lombok.Getter;
import uk.gov.hmcts.reform.pcs.ccd.event.EventId;

@Builder
@Getter
public class CcdPaymentStateUpdateTaskData {

    private long caseReference;
    private EventId eventId;

}
