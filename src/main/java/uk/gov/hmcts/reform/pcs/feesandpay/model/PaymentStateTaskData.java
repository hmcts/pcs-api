package uk.gov.hmcts.reform.pcs.feesandpay.model;

import lombok.Builder;
import lombok.Getter;
import uk.gov.hmcts.reform.pcs.ccd.event.EventId;
import uk.gov.hmcts.reform.pcs.system.SystemAction;

import java.util.UUID;

@Builder
@Getter
public class PaymentStateTaskData {

    private long caseReference;
    private EventId eventId;
    private String paymentReference;
    private UUID idempotencyKey;
    private SystemAction systemAction;

}
